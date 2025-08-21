
cmake_minimum_required(VERSION 3.22.1)
project(ptybridge)

add_library(ptybridge SHARED ptybridge.cpp)
find_library(log-lib log)
target_link_libraries(ptybridge ${log-lib})

# 16KB
target_link_options(ptybridge PRIVATE "-Wl,-z,max-page-size=16384")
