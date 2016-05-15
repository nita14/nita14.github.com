'use strict';
var beCycleApp = angular.module('beCycle', ['ngRoute','ngMaterial','esri.map']);
	beCycleApp.config(function($routeProvider) {
        $routeProvider

            .when('/', {
                templateUrl : 'pages/home.html',
                controller  : 'mainController'
            })

            .when('/mapCar', {
                templateUrl : 'pages/mapCar.html',
                controller  : 'mapCarController'
            })
			
			.when('/mapBike', {
                templateUrl : 'pages/mapBike.html',
                controller  : 'mapWalkController'
            })
			
			.when('/mapWalk', {
                templateUrl : 'pages/mapWalk.html',
                controller  : 'mapWalkController'
            })

            .when('/contact', {
                templateUrl : 'pages/contact.html',
                controller  : 'contactController'
            })
			
			.when('/selectParams', {
                templateUrl : 'pages/selectParams.html',
                controller  : 'selectParamsController'
            })
			
			.when('/showSummary', {
                templateUrl : 'pages/showSummary.html',
                controller  : 'showSummaryController'
            });
    });

    beCycleApp.controller('mainController', function($scope) {
        $scope.message = 'Would you like to commute more efficiently in Boston?';
    });

 
    beCycleApp.controller('selectParamsController', function($scope, $http, $window) {
		
		$scope.geoResults = 0;
		$scope.saved = localStorage.getItem('user');
		if ($scope.saved != null){		
			$scope.user= JSON.parse(localStorage.getItem('user'));
		} else{
			$scope.user={};
		}
	
		
		$scope.hours = ['5 am', '6 am', '7 am', '8 am', '9 am', '10 am', '11 am'];
		$scope.user.homeTime;
		$scope.getSelectedHour = function() {
        if ($scope.user.homeTime !== undefined) {
			  return $scope.user.homeTime;
			} else {
			  return "Please select an item";
			}
		};
		
		
		$scope.hoursWork = ['3 pm', '4 pm', '5 pm', '6 pm', '7 pm', '8 pm', '9 pm', '10 pm'];
		$scope.user.workTime;
		$scope.getSelectedHourWork = function() {
        if ($scope.user.workTime !== undefined) {
			  return $scope.user.workTime;;
			} else {
			  return "Please select an item";
			}
		};
		
		$scope.cars = ['Economy', 'Compact', 'Mid-size', 'MPV', 'SUV', 'Pick-up', 'Van'];
		$scope.user.car;
		$scope.getSelectedCar = function() {
        if ($scope.user.car !== undefined) {
			  return $scope.user.car;
			  
			} else {
			  return "Please select an item";
			}
		};
		
		$scope.summary = function() {
			$scope.homeGeocodeOK = $scope.geocode($scope.user.home, true, 0);
			$scope.geocode($scope.user.work, false, 1);
			localStorage.setItem('user', JSON.stringify($scope.user));
			
		
		}
		
		$scope.geocode = function(val, home, id){
			var url = 'http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/find?text=' + val + '&f=pjson' + '&callback=angular.callbacks._' + id;
	
				return $http.jsonp(url)
					.then(function(data){
						if (home){
							$scope.homeGPS = data.data.locations[0].feature.geometry;
							localStorage.setItem('homeGPS', JSON.stringify($scope.homeGPS));
						} else {
							$scope.workGPS= data.data.locations[0].feature.geometry;
							localStorage.setItem('workGPS', JSON.stringify($scope.workGPS)); 
						}
						$scope.geoResults = $scope.geoResults + 1; 
				});	
		}
		
		$scope.distanceCar =0;
		$scope.timeCar =0;
		$scope.geometryCar ={};
		$scope.distanceWalk =0;
		$scope.timeWalk =0;
		$scope.geometryWalk ={};
		$scope.timeBicycle=0;
		$scope.distanceBicycle =0;
		
		
		$scope.routingCalc = function(val, startTime, id, type){
			var url = 'http://utility.arcgis.com/usrsvcs/appservices/5n38Ew3krrK6phTw/rest/services/World/Route/NAServer/Route_World/solve?'
				+ 'stops=' + val + '&impedanceAttributeName=TravelTime&startTimeIsUTC=true' + '&startTime=' +startTime+ '&f=pjson' + '&callback=angular.callbacks._' + id;
	
				$http.jsonp(url)
					.success(function(data){
						if(type = 'car'){
							$scope.distanceCar = $scope.distanceCar + data.routes.features[0].attributes.Total_Kilometers;
							$scope.timeCar = $scope.timeCar + data.routes.features[0].attributes.Total_TravelTime;
							$scope.geometryCar = data.routes.features[0].geometry;
							localStorage.setItem('distanceCar', JSON.stringify($scope.distanceCar));
							localStorage.setItem('timeCar', JSON.stringify($scope.timeCar));
							localStorage.setItem('geometryCar', JSON.stringify($scope.geometryCar));
						}
						

				});	
		}
		
		$scope.$watch('geoResults' , function(){
			if ($scope.geoResults == 2){
				
				$scope.valRoutingTo = $scope.homeGPS.x + ',' + $scope.homeGPS.y + ';' + $scope.workGPS.x + ',' + $scope.workGPS.y;
				$scope.valRoutingFrom = $scope.workGPS.x + ',' + $scope.workGPS.y + ';' + $scope.homeGPS.x + ',' + $scope.homeGPS.y ;
				$scope.valBicycleFrom = $scope.homeGPS.y + ',' + $scope.homeGPS.x;
				$scope.valBicycleTo = $scope.workGPS.y+ ',' + $scope.workGPS.x;
				
				switch($scope.user.homeTime) {
					case '5 am':
						$scope.user.homeTimeMili='631170000000'
						break;
					case '6 am':
						$scope.user.homeTimeMili='631173600000'
						break;
					case '7 am':
						$scope.user.homeTimeMili='631177200000'
						break;
					case '8 am':
						$scope.user.homeTimeMili='631180800000'
						break;
					case '9 am':
						$scope.user.homeTimeMili='631184400000'
						break;
					case '10 am':
						$scope.user.homeTimeMili='631188000000'
						break;
					case '11 am':
						$scope.user.homeTimeMili='631191600000'
						break;
					default:
						$scope.user.homeTimeMili='631180800000'
				}
				
				switch($scope.user.workTime) {
					case '3 pm':
						$scope.user.workTimeMili='631206000000'
						break;
					case '4 pm':
						$scope.user.workTimeMili='631209600000'
						break;
					case '5 pm':
						$scope.user.workTimeMili='631213200000'
						break;
					case '6 pm':
						$scope.user.workTimeMili='631216800000'
						break;
					case '7 pm':
						$scope.user.workTimeMili='631220400000'
						break;
					case '8 pm':
						$scope.user.workTimeMili='631224000000'
						break;
					case '9 pm':
						$scope.user.workTimeMili='631227600000'
						break;
					default:
						$scope.user.workTimeMili='631220400000'
				}
				
				switch($scope.user.car) {
					case 'Economy':
						$scope.user.carType=110;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
						break;
					case 'Compact':
						$scope.user.carType=138;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
						break;
					case 'Mid-size':
						$scope.user.carType=195;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
						break;
					case 'MPV':
						$scope.user.carType=265;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
						break;
					case 'SUV':
						$scope.user.carType=218;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
						break;
					case 'Pick-up':
						$scope.user.carType=345;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
						break;
					case 'Van':
						$scope.user.carType=300;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
						break;
					default:
						$scope.user.carType=300;
						localStorage.setItem('carType', JSON.stringify($scope.user.carType));
				}
				
				$scope.routingCalc($scope.valRoutingTo,  $scope.user.homeTimeMili, 2, 'car')
				$scope.routingCalc($scope.valRoutingFrom, $scope.user.workTimeMili, 3, 'car')

				
				$scope.travelModeWalking = '{"restrictionAttributeNames":["Avoid Roads Unsuitable for Pedestrians","Preferred for Pedestrians","Walking"],"description":"Follows paths and roads that allow pedestrian traffic and finds solutions that optimize travel time. The walking speed is set to 5 kilometers per hour.","impedanceAttributeName":"WalkTime","simplificationToleranceUnits":"esriMeters","uturnAtJunctions":"esriNFSBAllowBacktrack","useHierarchy":false,"simplificationTolerance":2,"timeAttributeName":"WalkTime","distanceAttributeName":"Miles","type":"WALK","id":"caFAgoThrvUpkFBW","attributeParameterValues":[{"parameterName":"Restriction Usage","attributeName":"Walking","value":"PROHIBITED"},{"parameterName":"Restriction Usage","attributeName":"Preferred for Pedestrians","value":"PREFER_LOW"},{"parameterName":"Walking Speed (km/h)","attributeName":"WalkTime","value":8},{"parameterName":"Restriction Usage","attributeName":"Avoid Roads Unsuitable for Pedestrians","value":"AVOID_HIGH"}],"name":"Walking Time"}';
				$scope.walkingCalc($scope.valRoutingTo, $scope.travelModeWalking, 4, 'car')
				
			
				
			}
			
			
		});
		
		$scope.walkingCalc = function(val, mode, id, type){
			var url = 'http://utility.arcgis.com/usrsvcs/appservices/5n38Ew3krrK6phTw/rest/services/World/Route/NAServer/Route_World/solve?'
				+ 'stops=' + val + '&travelMode=' + mode + '&f=pjson' + '&callback=angular.callbacks._' + id;
	
				$http.jsonp(url)
					.success(function(data){
							$scope.distanceWalk = $scope.distanceWalk + data.routes.features[0].attributes.Total_Kilometers * 2;
							$scope.timeWalk = $scope.distanceWalk + data.routes.features[0].attributes.Total_WalkTime * 2;
							$scope.geometryWalk = data.routes.features[0].geometry;
							localStorage.setItem('distanceWalk', JSON.stringify($scope.distanceWalk));
							localStorage.setItem('timeWalk', JSON.stringify($scope.timeWalk));
							localStorage.setItem('geometryWalk', JSON.stringify($scope.geometryWalk));
										
							localStorage.setItem('distanceBicycle', JSON.stringify($scope.distanceWalk));
							localStorage.setItem('timeBicycle', JSON.stringify($scope.timeWalk)/2.1);
							
							$window.location.href = '#/showSummary';
						

				});	
				
		}
			
		
    });
	
	beCycleApp.controller('showSummaryController', function($scope, $http) {
			$scope.Math = window.Math;
			$scope.mode1 = "Biker"
			$scope.mode2 = "Walker"
			$scope.mode3 = "Car"
			$scope.bikeArray= [];
			$scope.walkArray= [];
			$scope.driveArray= [];
			
			if (localStorage.getItem('timeBicycle') != null){
				$scope.bikeArray.push(JSON.parse(localStorage.getItem('distanceBicycle'))/100*20);
				$scope.bikeArray.push(JSON.parse(localStorage.getItem('timeBicycle')));
				$scope.bikeArray.push(JSON.parse(localStorage.getItem('distanceBicycle')));
				$scope.bikeArray.push(JSON.parse(localStorage.getItem('timeBicycle'))*10);
				$scope.bikeArray.push(0);
			}
			
			if (localStorage.getItem('timeWalk') != null){
				$scope.walkArray.push(0);
				$scope.walkArray.push(JSON.parse(localStorage.getItem('timeWalk')));
				$scope.walkArray.push(JSON.parse(localStorage.getItem('distanceWalk')));
				$scope.walkArray.push(JSON.parse(localStorage.getItem('timeWalk'))*5);  //kcal
				$scope.walkArray.push(0);	
			}
			
			if (localStorage.getItem('timeCar') != null){
				$scope.driveArray.push(JSON.parse(localStorage.getItem('distanceCar'))/10*20);
				$scope.driveArray.push(JSON.parse(localStorage.getItem('timeCar')));
				$scope.driveArray.push(JSON.parse(localStorage.getItem('distanceCar')));
				$scope.driveArray.push(0);  //kcal
				$scope.driveArray.push(JSON.parse(localStorage.getItem('carType'))*localStorage.getItem('distanceCar')/100*20);	
			}
	
	   });
	   
	beCycleApp.controller('mapCarController', function($scope, esriLoader) {
		

		$scope.homeX = JSON.parse(localStorage.getItem('homeGPS')).x;
		$scope.homeY = JSON.parse(localStorage.getItem('homeGPS')).y;
		$scope.workX = JSON.parse(localStorage.getItem('workGPS')).x;
		$scope.workY = JSON.parse(localStorage.getItem('workGPS')).y;
		$scope.carGeom = JSON.parse(localStorage.getItem('geometryCar'));
		$scope.incidents =0
		
		$scope.map = {
			options: {
				basemap: 'dark-gray',
				center: [$scope.homeX,$scope.homeY],
				zoom: 13,
				sliderStyle: 'small',
			}
						
		};
		
		$scope.onMapLoad = function(map) {
			  
			   esriLoader.require([
                'esri/symbols/SimpleMarkerSymbol', 'esri/symbols/SimpleLineSymbol',
                'esri/symbols/PictureMarkerSymbol', 'esri/geometry/Point', 'esri/renderers/HeatmapRenderer', 'esri/layers/FeatureLayer', 'esri/geometry/Polyline',
                'esri/graphic', 'esri/geometry/geometryEngine', 'esri/tasks/query', 'esri/symbols/SimpleFillSymbol', 'esri/tasks/QueryTask',
                'esri/Color'
            ], function(
                SimpleMarkerSymbol, SimpleLineSymbol,
                PictureMarkerSymbol, Point, HeatmapRenderer, FeatureLayer, Polyline,
                Graphic, geometryEngine, Query, SimpleFillSymbol, QueryTask,
                Color
            ) {
				
				var serviceURL = "http://services.arcgis.com/rQ4nYWw3vwjxcr1D/arcgis/rest/services/IncidentBoston/FeatureServer/0";
					var heatmapFeatureLayerOptions = {
					  mode: FeatureLayer.MODE_SNAPSHOT
					};
					var heatmapFeatureLayer = new FeatureLayer(serviceURL, heatmapFeatureLayerOptions);
					var heatmapRenderer = new HeatmapRenderer();
					heatmapFeatureLayer.setRenderer(heatmapRenderer);
				map.addLayer(heatmapFeatureLayer);	
				
						
				var symbolLine = new SimpleLineSymbol(SimpleLineSymbol.STYLE_SHORTDOT, new Color([222,222,30]), 4);
				map.graphics.add(new Graphic(new Polyline($scope.carGeom), symbolLine));
				
				var symbolDom = new PictureMarkerSymbol("images/dom.png", 18, 18)
				map.graphics.add(new Graphic(new Point($scope.homeX,$scope.homeY), symbolDom));
				var symbolPraca = new PictureMarkerSymbol("images/work.png", 18, 18)
				map.graphics.add(new Graphic(new Point($scope.workX,$scope.workY), symbolPraca));
					
				var queryCircle = new Query();
				queryCircle.distance = 200;
				queryCircle.geometry = new Polyline($scope.carGeom);
				queryCircle.returnGeometry = false;
						
				var queryTask = new QueryTask(serviceURL)
					queryTask.executeForCount(queryCircle,function(count){
						$scope.incidents =count;
					},function(error){
						console.log(error);
					});

						
				});
						
			  
		  }
					 
		
	
	   });
	
	
	beCycleApp.controller('mapWalkController', function($scope, esriLoader) {
		

		$scope.homeX = JSON.parse(localStorage.getItem('homeGPS')).x;
		$scope.homeY = JSON.parse(localStorage.getItem('homeGPS')).y;
		$scope.workX = JSON.parse(localStorage.getItem('workGPS')).x;
		$scope.workY = JSON.parse(localStorage.getItem('workGPS')).y;
		$scope.carGeom = JSON.parse(localStorage.getItem('geometryWalk'));
		$scope.incidents =0
		
		$scope.map = {
			options: {
				basemap: 'dark-gray',
				center: [$scope.homeX,$scope.homeY],
				zoom: 13,
				sliderStyle: 'small',
			}
						
		};
		
		$scope.onMapLoad = function(map) {
			  
			   esriLoader.require([
                'esri/symbols/SimpleMarkerSymbol', 'esri/symbols/SimpleLineSymbol',
                'esri/symbols/PictureMarkerSymbol', 'esri/geometry/Point', 'esri/renderers/HeatmapRenderer', 'esri/layers/FeatureLayer', 'esri/geometry/Polyline',
                'esri/graphic', 'esri/geometry/geometryEngine', 'esri/tasks/query', 'esri/symbols/SimpleFillSymbol', 'esri/tasks/QueryTask',
                'esri/Color'
            ], function(
                SimpleMarkerSymbol, SimpleLineSymbol,
                PictureMarkerSymbol, Point, HeatmapRenderer, FeatureLayer, Polyline,
                Graphic, geometryEngine, Query, SimpleFillSymbol, QueryTask,
                Color
            ) {
				
				var serviceURL = "http://services.arcgis.com/rQ4nYWw3vwjxcr1D/arcgis/rest/services/IncidentBoston/FeatureServer/0";
					var heatmapFeatureLayerOptions = {
					  mode: FeatureLayer.MODE_SNAPSHOT
					};
					var heatmapFeatureLayer = new FeatureLayer(serviceURL, heatmapFeatureLayerOptions);
					var heatmapRenderer = new HeatmapRenderer();
					heatmapFeatureLayer.setRenderer(heatmapRenderer);
				map.addLayer(heatmapFeatureLayer);	
				
						
				var symbolLine = new SimpleLineSymbol(SimpleLineSymbol.STYLE_SHORTDOT, new Color([222,222,30]), 4);
				map.graphics.add(new Graphic(new Polyline($scope.carGeom), symbolLine));
				
				var symbolDom = new PictureMarkerSymbol("images/dom.png", 18, 18)
				map.graphics.add(new Graphic(new Point($scope.homeX,$scope.homeY), symbolDom));
				var symbolPraca = new PictureMarkerSymbol("images/work.png", 18, 18)
				map.graphics.add(new Graphic(new Point($scope.workX,$scope.workY), symbolPraca));
					
				var queryCircle = new Query();
				queryCircle.distance = 200;
				queryCircle.geometry = new Polyline($scope.carGeom);
				queryCircle.returnGeometry = false;
						
				var queryTask = new QueryTask(serviceURL)
					queryTask.executeForCount(queryCircle,function(count){
						$scope.incidents =count;
					},function(error){
						console.log(error);
					});

						
				});
						
			  
		  }
					 
		
	
	   });			
				
 
		

           

  