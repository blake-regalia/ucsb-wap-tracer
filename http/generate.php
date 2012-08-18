<?php

/*
//$cmd = 'cmd /C run.bat';
//shell_exec($cmd);
$WshShell = new COM("WScript.Shell");
$oExec = $WshShell->Run('run.bat', 0, false);
echo 'done';
exit;
*/

require("database.config.php");


$LOCAL = true;

/*** generate ***/

/** global vars **/

$unix;
if (substr(php_uname(), 0, 7) == "Windows") {
	$unix = false;
} else {
	$unix = true;
}

$JAVA_PATH = $unix? 'java': '"C:\\Program Files\\Java\\jre7\\bin\\java.exe"';
$MYSQL_PATH = $unix? 'mysql': '"D:\\HTTP\\xampp\\mysql\\bin\\mysql.exe"';
$SCRIPT_EXT = $unix? 'sh': 'bat';

$PHP_DATABASE_FILE = "database.php";

$DATABASE_NAME = $DATABASE['db_name'];
$WAPS_TABLE_NAME = "waps";
$EVENTS_TABLE_NAME = "events";

$ABSOLUTE_PATH = substr($_SERVER['SCRIPT_FILENAME'], 0, strrpos($_SERVER['SCRIPT_FILENAME'],'/'));

$arg_strict = (isset($_GET['strict']))? TRUE: FALSE;
$arg_pretty = (isset($_GET['pretty']))? '-pretty': '';

$arg_sql = $_GET['sql'];
$arg_json = $_GET['json'];

for($a=1; $a<$argc; $a++) {
	if(preg_match('/^\\-(\w+)$/', $argv[$a], $match)) {
		switch($match[1]) {
			case 'strict':
				$arg_strict = true;
				break;
			case 'pretty':
				$arg_pretty = true;
				break;
			case 'sql':
				$arg_sql = $argv[++$a];
				break;
			case 'json':
				$arg_json = $argv[++$a];
				break;
		}
	}
}


/** **/
function decode_bin($input) {
	global $JAVA_PATH;
	return shell_exec($JAVA_PATH.' -jar android-decoder.jar '.$input);
}

function decode_bin_to_file($input, $output) {
	global $JAVA_PATH;
	return shell_exec($JAVA_PATH.' -jar android-decoder.jar '.$input.' > '.$output);
}

function decode_bin_to_file_output_script($input, $output) {
	global $JAVA_PATH;
	return $JAVA_PATH.' -jar android-decoder.jar '.$input.' > '.$output;
}

function exec_in_background($cmd, $args = "") { 
	if (substr(php_uname(), 0, 7) == "Windows") {
		$WshShell = new COM("WScript.Shell");
		$oExec = $WshShell->Run($cmd, 0, false);
		return $oExec;
	} else { 
		exec("./".$cmd." > /dev/null &");
	} 
} 


function get_new_files($data_dir) {
	$array = array();
	return $array;
}

function get_all_files($data_dir, $ext) {
	$array = array();
	chdir($data_dir);
	$data_dir_files = scandir('.');
	foreach($data_dir_files as $user_dir) {
		if($user_dir != '.' && $user_dir != '..' && $user_dir[0] != '~') {
			if(chdir($user_dir) === false) continue;
			$user_dir_files = scandir('.');
			foreach($user_dir_files as $trace_file) {
				$full_path = $data_dir.'/'.$user_dir.'/'.$trace_file;
				$pi = pathinfo($full_path);
				if($pi['extension'] == $ext) {
					$array[] = array(
						'user' => $user_dir,
						'trace' => $trace_file,
						'path' => $data_dir.'/'.$user_dir.'/'.$trace_file
					);
				}
			}
			chdir('..');
		}
	}
	chdir('..');
	return $array;
}

function get_all_user_files($dir, $ext) {
	chdir($dir);
	$array = array();
	$user_dir_files = scandir('.');
	foreach($user_dir_files as $trace_file) {
		$full_path = $dir.'/'.$trace_file;
		$pi = pathinfo($full_path);
		if($pi['extension'] == $ext) {
			$array[] = array(
				'user' => $user_dir,
				'trace' => $trace_file,
				'path' => $dir.'/'.$trace_file
			);
		}
	}
	chdir('..');
	return $array;
}

function mysql_load_redirect($file) {
	header('Location: generate.php?sql='.$file);
	exit;
}

/****/


// json
if($arg_json) {
	if($arg_json == 'all') {
		
		// empty json directory
		if(!is_dir('json')) mkdir('json');
		else {
			chdir('json');
			$json_files = scandir('.');
			foreach($json_files as $file) {
				if($file != '.' && $file != '..') {
					unlink($file);
				}
			}
			chdir('..');
		}
		
		$files = get_all_files('data','bin');
	}
	else if($arg_json == 'new') {
		$files = get_new_files('data');
	}
	else if(is_dir('data/'.$_GET['json'])) {
		$data_dir = 'data/'.$_GET['json'];
		$files = get_all_user_files($data_dir);
	}
	else {
		die('wrong args');
	}
	
	$batch = array();
	
	ob_start();
	foreach($files as $file) {
		$json_path = 'json/'.$file['user'].'_'.$file['trace'].'.json';
		$batch[] = decode_bin_to_file_output_script('-json '.$arg_pretty.'"'.$file['path'].'"', $json_path);
		echo $json_path." <br />\n";
		ob_flush(); flush();
	}
	
	file_put_contents('json.'.$SCRIPT_EXT, implode("\n", $batch));
	exit;
}

// sql
else if(isset($arg_sql)) {
	
	if($arg_sql == 'all') {

		// empty sql directory
		if(!is_dir('sql')) {
			mkdir('sql');
			if(!is_dir('sql')) {
				die('failed to create directory');
			}
		}

		else {
			chdir('sql');
			$sql_files = scandir('.');
			foreach($sql_files as $file) {
				if($file != '.' && $file != '..') {
					unlink($file).'!';
				}
			}
			chdir('..');
		}
		
		$files = get_all_files('data','bin');
	}
	else if($arg_sql == 'new') {
		$files = get_new_files('data');
	}
	
	
	// load the database class
	require($PHP_DATABASE_FILE);
	
	// initialize an instance to database table
	$WAPs = new MySQL_Pointer($DATABASE_NAME);
	
	// delete the tables that are already there
	$WAPs->dropTable($WAPS_TABLE_NAME);
	$WAPs->dropTable($EVENTS_TABLE_NAME);

	$mysql = array();
	if($unix) {
		$mysql[] = '#!/bin/bash';
	}
	
	foreach($files as $file) {
		$sql_path = 'sql/'.$file['user'].'_'.$file['trace'].'.sql';
		decode_bin_to_file('-sql table='.$EVENTS_TABLE_NAME.' '.$arg_pretty.' "'.$file['path'].'"', $sql_path);
		
		$script = $MYSQL_PATH.' --user='.$DATABASE['USER'].' --password='.$DATABASE['PASS'].' "'.$DATABASE_NAME.'" < "'.$ABSOLUTE_PATH.'/'.$sql_path.'"';
		//shell_exec($script);
		echo $script."\n";
		
		$mysql[] = $script;
	}
	
	file_put_contents('mysql.'.$SCRIPT_EXT, implode("\n", $mysql));
	
	exit;
	
	/*
	
	else if(substr($arg,strrpos($arg,'.')) === '.sql') {
		$sql_files = scandir('sql');
		$sql_path = $arg;
		
		$script = $MYSQL_PATH.' --user='.$DATABASE['USER'].' --password='.$DATABASE['PASS'].' "'.$DATABASE_NAME.'" < "'.$ABSOLUTE_PATH.'/'.$sql_path.'"';
		shell_exec($script);
		/*
		
		$over = preg_split('/\n/', file_get_contents('sql/files.txt'));
		
		print_r($over);
		
		if(in_array($f, $sql_files))
			foreach($lines 
		foreach($sql_files as $file) {
			
		}
		file_put_contents('sql/files.txt', $target, FILE_APPEND);
		echo $arg;
		exit;*
	}
	*/
}


?>
