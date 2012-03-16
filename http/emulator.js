


$(document).ready(function() {
	GoogleMaps();
	
	var query = parseQuery(window.location.search);
	
	if(query.file) {
		new Trace(query.file);
	}
	else {
		
		WAP.hits(function(results) {
			var list = this;
			
			var sort = {};
			var max = 0;
			$.each(list, function(bs,m) {
				if(!sort[m]) sort[m] = [];
				sort[m].push(bs);
				if(m > max) max = m;
			});
			
	
			var waps = '';
			var i = max+1;
			while(i--) {
				var set = sort[i];
				if(set) {
					var n = set.length;
					while(n--) {
						var bssid = set[n];
						waps += '<div class="link" bssid="'+bssid+'">'+i+': '+bssid+'</div>';
					}
				}
			}
			$('#results').html(waps).find('.link').click(function() {
				WAP.click.apply(this).getSSID(Controls.setSSID);
			});
		
			if(query.ssid) {
				Controls.ssid(query.ssid);
			}
		});
	}
	
});

/** Controls **/
(function() {
	var __func__ = 'Controls';
	var self = {
		title: function(str) {
			$('#title').html(str);
		},
	};
	var global = window[__func__] = function() {
	};
	$.extend(global, {
		ssid: function(ssid) {
			global.setSSID(ssid);
			WAP.ssid(ssid, function(list) {
				var sort = {};
				var max = 0;
				$.each(list, function(k,v) {
					var m = WAP.numHits(v);
					if(!sort[m]) sort[m] = [];
					sort[m].push(v);
					if(m > max) max = m;
				});
				
				var waps = '';
				var i = max+1;
				while(i--) {
					var set = sort[i];
					if(set) {
						var n = set.length;
						while(n--) {
							var bssid = set[n];
							waps += '<div class="link" bssid="'+bssid+'">'+i+': '+bssid+'</div>';
						}
					}
				}
				$('#results').html(waps).find('.link').click(WAP.click);
			});
		},
		setSSID: function(ssid) {
			self.title('ssid: '+ssid);
		},
		toString: function() {
			return __func__+'()';
		}
	});
})();



var parseQuery = function(query) {
	var mode = 0;
	var tmp = '';
	var obj = {};
	var fn = {
		0: function(c) {
			if(c === '?' || c === '#') {
				tmp = '';
				mode = 1;
			}
		},
		1: function(c) {
			if(c === '=') {
				key = tmp.substr(0,tmp.length-1);
				tmp = '';
				mode = 2;
			}
			else if(c === '&') {
				obj[tmp.substr(0,tmp.length-1)] = true;
				tmp = '';
			}
		},
		2: function(c) {
			if(c === '&') {
				obj[key] = tmp.substr(0,tmp.length-1);
				tmp = '';
				mode = 1;
			}
		},
	};
	for(var i=0; i!==query.length; i++) {
		var c = query[i];
		tmp += c;
		fn[mode](c);
	}
	if(mode === 1) {
		obj[tmp] = true;
	}
	else if(mode === 2) {
		obj[key] = tmp;
	}
	return obj;
};


/** ColorGradient */
(function() {
	var __func__ = 'ColorGradient';
	var construct = function(a, b) {
		
		var self = {
			rgb: function(p) {
				return {
					r: Math.round(a.r+(b.r-a.r)*p),
					g: Math.round(a.g+(b.g-a.g)*p),
					b: Math.round(a.b+(b.b-a.b)*p)
				};
			},
		};
		var public = function() {
			
		};
		$.extend(public, {
			
			rgb: function(p) {
				return 'rgb('+(a.r+(b.r-a.r)*p)+','+(a.g+(b.g-a.g)*p)+','+(a.b+(b.b-a.b)*p)+')';
			},
			hex: function(p) {
				var rgb = self.rgb(p);
				var r = rgb.r.toString(16);
				while(r.length < 2) r = '0'+r;
				
				var g = rgb.g.toString(16);
				while(g.length < 2) g = '0'+g;
				
				var b = rgb.b.toString(16);
				while(b.length < 2) b = '0'+b;
				
				return '#'+r+g+b;
			},
		});
		return public;
	};
	var global = window[__func__] = function() {
		if(this !== window) {
			var instance = construct.apply(this, arguments);
			return instance;
		}
		else {
			
		}
	};
	$.extend(global, {
		toString: function() {
			return __func__+'()';
		}
	});
})();



(function() {
	var __func__ = 'WAP';
	var heat = new ColorGradient(
		{r:0, g:0, b:255},
		{r:255, g:0, b:0}
	);
	var circle = {
		radius: 2,
	};
	var construct = function(bssid) {
		circle.map = GoogleMaps.getMap();
		
		var overlays = [];
		var self = {
			draw: function(data) {
				var gradient = heat;
				var color;
				for(var i in data) {
					var e = data[i];
					var p = new google.maps.LatLng(e.lat, e.lon);
					
					color = gradient.hex(e.rssi/255);
					
					circle.fillColor = color;
					circle.strokeColor = color;
					circle.center = p;
					
					overlays.push(new google.maps.Circle(circle));
				}
			},
		};
		var public = function() {
			
		};
		$.extend(public, {
			destroy: function() {
				var i = overlays.length;
				while(i--) {
					overlays[i].setMap(null);
				}
			},
			getSSID: function(callback) {
				$.ajax({
					url: 'index.php?bssid='+bssid+'&ssid',
					dataType: 'json',
					success: function(json) {
						callback.apply({}, [json]);
					},
				});
			},
		});
		$.ajax({
			url: 'index.php?bssid='+bssid,
			dataType: 'json',
			error: function(e) {
				console.error(global,': '+e);
			},
			success: function(json) {
				self.draw(json);
			},
		});
		return public;
	};
	var global = window[__func__] = function() {
		if(this !== window) {
			var instance = construct.apply(this, arguments);
			return instance;
		}
		else {
			
		}
	};
	
	var wap = {destroy:function(){}};
	var hits = false;
	$.extend(global, {
		
		all: function(callback) {
			$.ajax({
				url: 'index.php?bssid=*',
				dataType: 'json',
				error: function(e) {
					console.error(global,': '+e);
				},
				success: function(json) {
					callback.apply(json, [{
						on: function(ssid) {
							var r = {};
							for(var bssid in json) {
								if(json[bssid] == ssid) {
									r[bssid] = json[bssid];
								}
							}
							return r;
						},
					}]);
				},
			});
		},
		
		ssid: function(ssid, callback) {
			$.ajax({
				url: 'index.php?ssid='+ssid,
				dataType: 'json',
				error: function(e) {
					console.error(global,': '+e);
				},
				success: function(json) {
					callback.apply({}, [json]);
				},
			});
		},
		
		numHits: function(bssid) {
			if(hits[bssid]) {
				return hits[bssid];
			}
			return 0;
		},
		
		hits: function(callback) {
			$.ajax({
				url: 'index.php?bssid=*&hits',
				dataType: 'json',
				error: function(e) {
					console.error(global,': '+e);
				},
				success: function(json) {
					hits = json;
					callback.apply(json, [{
						over: function(n) {
							var r = {};
							for(var bssid in json) {
								if(json[bssid] > n) {
									r[bssid] = json[bssid];
								}
							}
							return r;
						},
						
						all: function() {
							return hits;
						},
						
					}]);
				},
			});
		},
		load: function(bssid) {
			wap.destroy();
			wap = new WAP(bssid);
			return wap;
		},
		click: function() {
			$('.selected').removeClass('selected');
			$(this).addClass('selected');
			return WAP.load($(this).attr('bssid'));
		},
		toString: function() {
			return __func__+'()';
		}
	});
})();


var shift_lat = 0;
var shift_lon = 0;

/** GoogleMaps **/
(function() {
	var map = false;
	var overlays = [];
	
	var mwlog;
	
	var stringify = function(event) {
		var i = event.length;
		var b = '';
		while(i--) {
			b += (Math.round((event[i].rssi/255)*1000)/10)+'% '+mwlog.bssids[event[i].mac]+' * '+mwlog.ssids[event[i].ssid]+'\n';
		}
		return b;
	};

	var self = {
		randomColor: function() {
			
			return ['hsl(',
				Math.round(Math.random()*360),',',
				Math.round(Math.random()*70+30),'%,',
				Math.round(Math.random()*65),'%',
			')'].str()
		},
	};
		
	var global = window.GoogleMaps = function() {
		var latlng = new google.maps.LatLng(
			34.414699,-119.846195
		);
			
		var options = {
			zoom: 17,
			mapTypeId: 'roadmap',
			center: latlng,
			streetViewControl: false,
		};
		
		map = new google.maps.Map(
			$('#map_canvas')[0],
			options
		);
		
	};
	$.extend(global, {
		getMap: function() {
			return map;
		},
		clean: function() {
			var i=overlays.length;
			while(i--) {
				overlays[i].setMap(null);
			}
			overlays.length = 0;
		},
		save_trace: function(wlog) {
			console.log(wlog);
			mwlog = wlog;
		},
		
		traceBSSID: function(k) {
			global.clean();
			var wlog = mwlog;
			
			var marker = {setMap:function(){}};
			
			var marker_opt = {
				map: map,
			};
			
			var buffer = {
				clickable: true,
				fillOpacity: 0.02,
				map: map,
				strokeOpacity: 0.05,
				fillColor: 'blue',
			};
			var line = {
				map: map,
				strokeColor: 'red',
				strokeOpacity: 0.5,
			};
			var grad = new ColorGradient(
				{r:255, g:0, b:0},
				{r:0, g:0, b:255}
			);
			var dh = 0.00003;
			var i = wlog.length;
			var path = new google.maps.MVCArray();
			var last_pt = false;
			var time_span = wlog[wlog.length-1].time-wlog.start_time;
			while(i--) {
				var event = wlog[i];
				var spot = new google.maps.LatLng(event.latitude+shift_lat, event.longitude+shift_lon);
				
				if(event.accuracy <= 15) {
					
					var tmp = last_pt;
					last_pt = false;
					var b = event.length;
					while(b--) {
						if(event[b].mac === k) {
							if(tmp) {
								var path = new google.maps.MVCArray();
								path.push(tmp);
								path.push(spot);
								var pct = 1-(event[b].rssi/rssi_max);
								
								overlays.push(new google.maps.Polyline({
									map: map,
									path: path,
									strokeColor: grad.hex(pct),
									strokeOpacity: 0.7,
									clickable: true,
									strokeWeight: 6,
								}));
							}
							last_pt = spot;
							break;
						}
					}
				}
			}
			/*
			new google.maps.Polyline({
				map: map,
				path: path,
				strokeColor: self.randomColor(),
				strokeOpacity: 0.7,
			});
			*/
			
			google.maps.event.addListener(map, 'mousemouse', function(e) {
				console.log(e);
			});
		},
	});
	
})();

(function() {
	var __func__ = 'Trace';
	
	var construct = function(uri) {
		var _wlog;
		var overlays = [];
		var map;
		var self = {
			trace: function(wlog) {
				public.clean();
				_wlog = wlog;
				
				console.log(wlog);
				
				var marker = {setMap:function(){}};
				
				var marker_opt = {
					map: map,
				};
				
				var buffer = {
					clickable: true,
					fillOpacity: 0.02,
					map: map,
					strokeOpacity: 0.05,
					fillColor: 'blue',
				};
				var line = {
					map: map,
					strokeColor: 'red',
					strokeOpacity: 0.5,
				};
				var grad = new ColorGradient(
					{r:0, g:0, b:255},
					{r:255, g:0, b:0}
				);
				var dh = 0.00003;
				var i = wlog.length;
				var path = new google.maps.MVCArray();
				var last_pt = false;
				var time_span = wlog[wlog.length-1].time-wlog.start_time;
				while(i--) {
					var event = wlog[i];
					console.log(event);
					var spot = new google.maps.LatLng(event.latitude+shift_lat, event.longitude+shift_lon);
					//path.push(spot);
					
					if(event.accuracy <= 15) {
						
						
						buffer.center = spot;
						buffer.radius = event.accuracy;
						var buf = new google.maps.Circle(buffer);
						overlays.push(buf);
						var f = (function() {
							var my = this;
							return function() {
								marker.setMap(null);
								marker_opt.position = my.loc;
								marker = new google.maps.Marker(marker_opt);
								//console.log(stringify(my.info));
							};
						}).apply({loc:spot,info:event});
						
						
						new google.maps.event.addListener(buf, 'click', f);
						
						if(last_pt) {
							var path = new google.maps.MVCArray();
							path.push(last_pt);
							path.push(spot);
							overlays.push(new google.maps.Polyline({
								map: map,
								path: path,
								strokeColor: grad.hex((event.time-wlog.start_time)/time_span),
								strokeOpacity: 0.7,
								clickable: true,
								strokeWeight: 6,
							}));
						}
						last_pt = spot;
					}
					else {
						var hatch = new google.maps.MVCArray();
						hatch.push(new google.maps.LatLng(event.latitude-dh, event.longitude-dh));
						hatch.push(new google.maps.LatLng(event.latitude+dh, event.longitude+dh));
						line.path = hatch;
						overlays.push(new google.maps.Polyline(line));
								
						var hatch = new google.maps.MVCArray();
						hatch.push(new google.maps.LatLng(event.latitude+dh, event.longitude-dh));
						hatch.push(new google.maps.LatLng(event.latitude-dh, event.longitude+dh));
						line.path = hatch;
						overlays.push(new google.maps.Polyline(line));
					}
				}
				
				google.maps.event.addListener(map, 'mousemove', function(e) {
					console.log(e);
				});
			},
		};
		var public = function() {
			
		};
		$.extend(public, {
			clean: function() {
				var i = overlays.length;
				while(i--) {
					overlays[i].setMap(null);
				}
			},
		});
		
		map = GoogleMaps.getMap();
		
		$.ajax({
			url: 'json/'+uri,
			dataType: 'json',
			error: function(e) {
				console.error(global,': ',e);
			},
			success: self.trace,
		});
		
		return public;
	};
	var global = window[__func__] = function() {
		if(this !== window) {
			var instance = construct.apply(this, arguments);
			return instance;
		}
		else {
			
		}
	};
	$.extend(global, {
		toString: function() {
			return __func__+'()';
		},
		
		randomColor: function() {
			
			return ['hsl(',
				Math.round(Math.random()*360),',',
				Math.round(Math.random()*70+30),'%,',
				Math.round(Math.random()*65),'%',
			')'].str()
		},
	
	});
})();