cmake_minimum_required(VERSION 3.22.1)
project(linkbubble-adblock)

set(CMAKE_CXX_STANDARD 14)

add_library(LinkBubble SHARED
    main.cpp
    ABPFilterParser.cpp
    cosmeticFilter.cpp
    filter.cpp
    BloomFilter.cpp
    hashFn.cpp
    HashSet.cpp
    TPParser.cpp
    TrackerData.cpp
    FirstPartyHost.cpp
)

target_include_directories(LinkBubble PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

find_library(log-lib log)

target_link_libraries(LinkBubble ${log-lib})
