<?php
$JAVA_PATH = '"C:\\Program Files\\Java\\jre7\\bin\\java.exe"';
$MYSQL_PATH = '"D:\\HTTP\\xampp\\mysql\\bin\\mysql.exe"';
$PHP_DATABASE_FILE = "database.php";
$DATABASE_NAME = "ucsb_wap_tracer";
$WAPS_TABLE_NAME = "waps";
$EVENTS_TABLE_NAME = "events";
$ABSOLUTE_PATH = substr($_SERVER['SCRIPT_FILENAME'], 0, strrpos($_SERVER['SCRIPT_FILENAME'],'/'));
$STRICT = FALSE;

function decode_bin($input) {
	global $JAVA_PATH;
	return shell_exec($JAVA_PATH.' -jar android-decoder.jar '.$input);
}

function decode_bin_to_file($input, $output) {
	global $JAVA_PATH;
	return shell_exec($JAVA_PATH.' -jar android-decoder.jar '.$input.' > '.$output);
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

function current_waps() {
	$ANY = !!isset($_GET['all']);
	
	$SSIDs = array(
		'UCSB Wireless Web',
	);
	
	
	$wifi = preg_split('/\n/', shell_exec('netsh wlan show networks mode=bssid'));
	
	$indexed_grep = preg_grep('/SSID|Signal/',$wifi);
	
	$length = sizeof($indexed_grep);
	
	$indicies = array_keys(array_fill(0, $length, 1));
	
	$grep = array_combine($indicies, $indexed_grep);
	
	$i = -1;
	while($i++ < $length) {
		$line = $grep[$i];
		if(preg_match('/^SSID [0-9]+ : (.*)/', $grep[$i], $capture)) {
			if($ANY || in_array($capture[1], $SSIDs)) {
				echo $line."\n";
				while($i++ < $length) {
					$line = $grep[$i];
					if(preg_match('/^SSID/', $line)) {
						break;
					}
					echo $line."\n";
				}
				//break;
			}
		}
	}
}

$trace_name_pattern = '^\d+\.\d+\.\d+\-\d+\.\d+\.\d+';

if(sizeof($_FILES) !== 0) {
	$trace_file = 'traces/'.basename($_FILES['trace']['name']);
	
	$client_hash = substr(md5($_SERVER['REMOTE_ADDR'].':'.$_SERVER['REMOTE_PORT'].'@'.$_SERVER['REQUEST_TIME']), 2, 12);
	$json_file = $client_hash.'.json';
	
	if(!$STRICT || preg_match('/'.$trace_name_pattern.'\.bin$/', basename($_FILES['trace']['name']))) {
		if(move_uploaded_file($_FILES['trace']['tmp_name'], $trace_file)) {
			$json_file = basename($trace_file,'.bin').'.json';
			decode_bin_to_file('-json '.$trace_file, 'json/'.$json_file);
			header('Location: '.$_SERVER['QUERY_STRING'].'?file='.$json_file);
			exit;
		}
		else {
			echo 'Upload failed. Please retry...';
			exit;
		}
	}
	else {
		echo 'File "'.basename($_FILES['trace']['name']).'" does not follow naming convention: YEAR.MONTH.DAY-HOUR.MINUTE.SECOND.bin';
		exit;
	}
}
else if(isset($_GET['file']) || isset($_GET['view'])) {
	readfile('index.html');
	exit;
}

// data
else if(isset($_GET['bssid'])) {
	
	// load the database class
	include_once($PHP_DATABASE_FILE);
	
	// initialize an instance to database table
	$WAPs = new MySQL_Pointer($DATABASE_NAME);
	
	$arg = $_GET['bssid'];
	
	// what is the script asking for
	if($arg == '*') {
		$object = 'ssid';
		if(isset($_GET['hits'])) {
			//$target = 'hits';
			$object = 'hits';
		}
		else if(isset($_GET['ssid'])) {
			$target = 'ssid';
		}
		else if(isset($_GET['hw_addr'])) {
			$target = 'bssid';
		}
		
		if(isset($target)) {
			$WAPs->selectTable($WAPS_TABLE_NAME);
			$table = $WAPs->fetchAssoc();
			$json = array();
			foreach($table as $row) {
				$json[] = $row[$target];
			}
			echo json_encode($json);
		}
		else {
			$WAPs->selectTable($WAPS_TABLE_NAME);
			$table = $WAPs->fetchAssoc();
			$json = array();
			foreach($table as $row) {
				$json[$row['bssid']] = $row[$object];
			}
			echo json_encode($json);
		}
	}
	else if(isset($_GET['ssid'])) {
		$WAPs->selectTable($WAPS_TABLE_NAME);
		$row = $WAPs->fetchRow('bssid', $arg);
		echo json_encode($row['ssid']);
	}
	else if(isset($_GET['hits'])) {
		$WAPs->selectTable($EVENTS_TABLE_NAME);
		$size = $WAPs->fetchSizeWhereEquals('bssid', $arg);
		echo json_encode($size);
	}
	else {
		$WAPs->selectTable($EVENTS_TABLE_NAME);
		$records = $WAPs->fetchAssocWhereEquals('bssid', $arg);
		$json = array();
		foreach($records as $event) {
			if($event['accuracy'] <= 10) {
				$json[] = array(
					'rssi' => $event['rssi'],
					'lat' => $event['latitude'],
					'lon' => $event['longitude']
				);
			}
		}
		echo json_encode($json);
	}
	
	exit;
	
}

// ssid
else if(isset($_GET['ssid'])) {
	
	// load the database class
	include_once($PHP_DATABASE_FILE);
	
	// initialize an instance to database table
	$WAPs = new MySQL_Pointer($DATABASE_NAME);
	
	$WAPs->selectTable($WAPS_TABLE_NAME);
	$table = $WAPs->fetchAssocWhereEquals('ssid',$_GET['ssid']);
	$json = array();
	foreach($table as $row) {
		$json[] = $row['bssid'];
	}
	echo json_encode($json);
	exit;
}

else if(isset($_GET['jsons'])) {
	$traces = scandir('json');
	$jsons = array();
	foreach($traces as $file) {
		if($file != '.' && $file != '..') {
			$jsons[] = $file;
		}
	}
	echo json_encode($jsons);
}
else {
	readfile('upload.html');
	exit;
}



?>