cmake_minimum_required (VERSION 2.6)
project (cDarkClient)

include_directories(..)

set (FILES buffer.c   
	channel.h     
	connection.h  
	error_codes.h  
	io_utils.c    
	message.c   
	session.c      
	socket.c
	buffer.h   
	config.h      
	context.c     
	id.h           
	linked_map.c  
	socket.h
	channel.c  
	connection.c  
	context.h     
	id_impl.c      
	map.h         
	protocol.h  
	session.h
	private/buffer_impl.h
	private/channel_impl.h
	private/connection_impl.h
	private/context_impl.h
	private/io_utils.h
	private/message.h
	private/session_impl.h
)

add_library(${PROJECT_NAME} ${FILES})
