#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>

static pid_t g_pid = -1;
static int   g_master = -1;

static int open_pty(int* master_fd, int* slave_fd) {
    int m = posix_openpt(O_RDWR | O_NOCTTY);
    if (m < 0) return -1;
    if (grantpt(m) != 0 || unlockpt(m) != 0) { close(m); return -1; }
    char* name = ptsname(m);
    if (!name) { close(m); return -1; }
    int s = open(name, O_RDWR | O_NOCTTY);
    if (s < 0) { close(m); return -1; }
    *master_fd = m; *slave_fd = s;
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_androshell_androshell_NativePty_startProcess(
        JNIEnv* env, jclass /*clazz*/,
        jstring jcmd, jstring jcwd, jobjectArray jenvp) {

    const char* cmd = env->GetStringUTFChars(jcmd, nullptr);
    const char* cwd = env->GetStringUTFChars(jcwd, nullptr);

    int envc = jenvp ? env->GetArrayLength(jenvp) : 0;
    char** envp = (char**)calloc(envc + 1, sizeof(char*));
    for (int i = 0; i < envc; i++) {
        jstring je = (jstring)env->GetObjectArrayElement(jenvp, i);
        const char* ce = env->GetStringUTFChars(je, nullptr);
        envp[i] = strdup(ce);
        env->ReleaseStringUTFChars(je, ce);
        env->DeleteLocalRef(je);
    }
    envp[envc] = nullptr;

    int mfd, sfd;
    if (open_pty(&mfd, &sfd) != 0) {
        for (int i = 0; i < envc; i++) free(envp[i]);
        free(envp);
        env->ReleaseStringUTFChars(jcmd, cmd);
        env->ReleaseStringUTFChars(jcwd, cwd);
        return nullptr;
    }

    pid_t pid = fork();
    if (pid == 0) {
        setsid();
        ioctl(sfd, TIOCSCTTY, 0);
        dup2(sfd, 0); dup2(sfd, 1); dup2(sfd, 2);
        close(mfd);
        if (cwd && *cwd) chdir(cwd);
        char* const argv[] = {(char*)cmd, nullptr};
        execve(cmd, argv, envp);
        _exit(127);
    }

    g_pid = pid; g_master = mfd; close(sfd);

    jclass pfdCls = env->FindClass("android/os/ParcelFileDescriptor");
    jmethodID adopt = env->GetStaticMethodID(
            pfdCls, "adoptFd", "(I)Landroid/os/ParcelFileDescriptor;");
    jobject pfd = env->CallStaticObjectMethod(pfdCls, adopt, mfd);

    for (int i = 0; i < envc; i++) free(envp[i]);
    free(envp);
    env->ReleaseStringUTFChars(jcmd, cmd);
    env->ReleaseStringUTFChars(jcwd, cwd);
    return pfd;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_androshell_androshell_NativePty_setWindowSize(
        JNIEnv* /*env*/, jclass /*clazz*/, jint cols, jint rows) {
    if (g_master < 0) return -1;
    struct winsize ws{};
    ws.ws_col = (unsigned short)cols;
    ws.ws_row = (unsigned short)rows;
    return ioctl(g_master, TIOCSWINSZ, &ws);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_androshell_androshell_NativePty_pid(
        JNIEnv* /*env*/, jclass /*clazz*/) {
    return (jint)g_pid;
}
