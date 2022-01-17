#!/bin/bash
# GENERATOR RUCHU PO HTTP #
# 10.0.0.4 jest adresem load balancera do serwerow http modulu python (python3 -m http.server) #
# za pomoca wget mozemy generowac ruch pobierajac dowolne pliki z serwera #

while true
do
	number=$(expr $RANDOM % 3)
	if [ $number == 0 ];
	then
		wget http://10.0.0.4:8000/file1 -O /dev/null
	elif [ $number == 1 ];
	then
		wget http://10.0.0.4:8000/file2 -O /dev/null
	else
		wget http://10.0.0.4:8000/file3 -O /dev/null
	fi
	
	sleep .$[ ( $RANDOM % 9 ) + 1 ]s
    sleep $[ ( $RANDOM % 2 ) ]s

done
