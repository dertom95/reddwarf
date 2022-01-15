cmake_minimum_required (VERSION 2.6)
project (darkChatClient)

include_directories(..)
link_libraries(cDarkClient)

set (FILES chat_client.c)

add_executable(${PROJECT_NAME} ${FILES})
