cmake_minimum_required(VERSION 2.6)

project(test)

include_directories(..)
link_libraries(cDarkClient)

add_executable( buffer_test.c)
add_executable( id_test.c)
add_executable( map_test.c)
add_executable( message_test.c)
add_executable( testCallbacks.c smokeTestClient.c )

