#!/bin/bash

mac=$1

hash=$(echo "$1" | md5sum | grep -Eo "[0-9a-z]+")

hash_len=${#hash}

length=10
offset=0

endset=$(( $hash_len - $length ))

while [ $offset -lt $endset ]; do
	md5=${hash:offset:length}
	res=$(cat "AP-Login-Sample.csv" | grep "$md5")
	echo "$md5:"
	#if [ -z "$res"]; then
		echo "$res"
	#fi
	
	offset=$((offset + 1))
done