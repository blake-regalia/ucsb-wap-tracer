<?php

$is_register = (
	isset($_POST['android-id'])
	&& isset($_POST['phone-number'])
);

$is_upload = (
	isset($_POST['version']) 
	&& isset($_POST['android-id'])
	&& isset($_POST['data'])
	&& isset($_POST['name'])
);

if(!$is_register && !$is_upload) {
	file_put_contents('reject.log', 'not all fields set.'.print_r($_POST,true)."\n\n", FILE_APPEND);
	header('HTTP/1.0 403 Forbidden');
	exit;
}

$version = $_POST['version'];
$android_id = $_POST['android-id'];
$phone_number = $_POST['phone-number'];
$encoded_data = $_POST['data'];
$file_name = $_POST['name'];

$android_id = preg_replace('/^[\\.\/]*/', '', $android_id);

if($is_upload) {
	$file_name = preg_replace('/^[\\.\/]*/', '', $file_name);
	$data = base64_decode($encoded_data);
}

if(!is_dir('data')) {
	mkdir('data');
}
chdir('data');

if(!is_dir($android_id)) {
	mkdir($android_id);
}
chdir($android_id);

if(!is_file('user-info.txt') && isset($phone_number)) {
	file_put_contents('user-info.txt', $android_id.':'.$phone_number);
}

if($is_register && !$is_upload) {
	echo file_get_contents('user-info.txt');
	exit;
}

if($is_upload) {
	$bytes_written = (int) (file_put_contents($file_name, $data));
	echo $bytes_written;
	exit;
}


?>