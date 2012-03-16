<?php

/*** generate ***/

/** global vars **/
$JAVA_PATH = '"C:\\Program Files\\Java\\jre7\\bin\\java.exe"';
$MYSQL_PATH = '"D:\\HTTP\\xampp\\mysql\\bin\\mysql.exe"';

$PHP_DATABASE_FILE = "database.php";

$DATABASE_NAME = "ucsb_wap_tracer";
$WAPS_TABLE_NAME = "waps";
$EVENTS_TABLE_NAME = "events";

$ABSOLUTE_PATH = substr($_SERVER['SCRIPT_FILENAME'], 0, strrpos($_SERVER['SCRIPT_FILENAME'],'/'));

$STRICT = (isset($_GET['strict']))? TRUE: FALSE;
$PRETTY = (isset($_GET['pretty']))? '-pretty': '';


/** **/
function decode_bin($input) {
	global $JAVA_PATH;
	return shell_exec($JAVA_PATH.' -jar android-decoder.jar '.$input);
}

function decode_bin_to_file($input, $output) {
	global $JAVA_PATH;
	return exec_in_background($JAVA_PATH, ' -jar android-decoder.jar '.$input.' > '.$output);
}

function exec_in_background($exe, $args = "") { 
	if (substr(php_uname(), 0, 7) == "Windows"){ 
		$cmd = "start \"bla\" " . $exe . " " . $args;
		pclose(popen($cmd, "r"));
		return $cmd;
	} else { 
		exec("./" . $exe . " " . escapeshellarg($args) . " > /dev/null &");    
	} 
} 


function get_all_files($data_dir) {
	$array = array();
	chdir($data_dir);
	$data_dir_files = scandir('.');
	foreach($data_dir_files as $user_dir) {
		if($user_dir != '.' && $user_dir != '..') {
			chdir($user_dir);
			$user_dir_files = scandir('.');
			foreach($user_dir_files as $trace_file) {
				if($trace_file != '.' && $trace_file != '..') {
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


/****/


// json
if(isset($_GET['json'])) {
	if($_GET['json'] == 'all') {
		
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
		
		$files = get_all_files('data');
		
		ob_start();
		foreach($files as $file) {
			$json_path = 'json/'.$file['user'].'_'.$file['trace'].'.json';
			decode_bin_to_file('-json '.$PRETTY.'"'.$file['path'].'"', $json_path);
			echo $json_path;
			ob_flush(); flush();
		}
		exit;
	}
}

// sql
else if(isset($_GET['sql'])) {
	
	if($_GET['sql'] == 'all') {
		
		// empty sql directory
		if(!is_dir('sql')) mkdir('sql');
		else {
			chdir('sql');
			$sql_files = scandir('.');
			foreach($sql_files as $file) {
				if($file != '.' && $file != '..') {
					unlink($file);
				}
			}
			chdir('..');
		}
		
		// load the database class
		include_once($PHP_DATABASE_FILE);
		
		// initialize an instance to database table
		$WAPs = new MySQL_Pointer($DATABASE_NAME);
		
		$files = get_all_files('data');
		
		// delete the tables that are already there
		$WAPs->dropTable($WAPS_TABLE_NAME);
		$WAPs->dropTable($EVENTS_TABLE_NAME);
		
		ob_start();
		foreach($files as $file) {
			$sql_path = 'sql/'.$file['user'].'_'.$file['trace'].'.sql';
			echo decode_bin_to_file('-sql table='.$EVENTS_TABLE_NAME.' '.$PRETTY.' "'.$file['path'].'"', $sql_path);
			echo "\n";
			$script = $MYSQL_PATH.' --user='.$DATABASE['USER'].' --password='.$DATABASE['PASS'].' "'.$DATABASE_NAME.'" < "'.$ABSOLUTE_PATH.'/'.$sql_path.'"';
			shell_exec($script);
			echo $script."\n";
			ob_flush(); flush();
		}
		exit;
	}
}


?>