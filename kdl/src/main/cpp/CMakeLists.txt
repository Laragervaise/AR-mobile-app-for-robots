cmake_minimum_required(VERSION 3.4.1)

project(kdljni)


set(EIGEN3_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../libraries/eigen") 
set(HAVE_STL_CONTAINER_INCOMPLETE_TYPES FALSE)
set(KDL_USE_NEW_TREE_INTERFACE_DEFAULT Off)


set(KDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../libraries/kdl")


file(GLOB_RECURSE KDL_SRCS
	${KDL_SOURCE_DIR}/kdl/[^.]*.cpp
	${KDL_SOURCE_DIR}/kdl/[^.]*.cxx
)

file(GLOB KDL_HEADERS
	${KDL_SOURCE_DIR}/kdl/[^.]*.hpp
	${KDL_SOURCE_DIR}/kdl/[^.]*.inl
)

file(GLOB KDL_UTIL_HEADERS
	${KDL_SOURCE_DIR}/kdl/utilities/[^.]*.h
	${KDL_SOURCE_DIR}/kdl/utilities/[^.]*.hpp
)


CONFIGURE_FILE(${KDL_SOURCE_DIR}/kdl/config.h.in config.h @ONLY)


include_directories(${EIGEN3_INCLUDE_DIR})
include_directories(${PROJECT_BINARY_DIR})

add_library(orocos-kdl STATIC ${KDL_SRCS} ${KDL_HEADERS} ${KDL_UTIL_HEADERS} config.h)


set(KDLJNI_SRCS
	kdl.cpp
	robot.cpp
	utils.cpp
)

set(KDLJNI_HEADERS
	robot.h
	utils.h
)

include_directories(${KDL_SOURCE_DIR})

add_library(KDLJni SHARED ${KDLJNI_SRCS} ${KDLJNI_HEADERS})

target_link_libraries(KDLJni orocos-kdl)
