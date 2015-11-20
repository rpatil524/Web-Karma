var ClassDialog = (function() {

	var instance = null;


	function PrivateConstructor() {
		var dialog = $("#classDialog");

		var worksheetId, columnId;
		var columnUri, columnLabel, columnDomain, columnCategory, alignmentId;
		var nodeType, isUri; //LiteralNode or InternalNode
		var allClassCache;

		function init() {
			allClassCache = null;
			window.setTimeout(function() {
				allClassCache = getAllClasses(worksheetId);
			}, 10);
			$('input', dialog).on('keyup', filterDropdown);
		}

		function hide() {
			dialog.modal('hide');
		}

		function hideError() {
			$("div.error", dialog).hide();
		}

		function showError(err) {
			if (err) {
				$("div.error", dialog).text(err);
			}
			$("div.error", dialog).show();
		}

		function selectFromMenu(e) {
			target = $(e.target);
			label = target.text();
			

			console.log("Selected class:" + label);
			if(label == 'More...') {
				populateAll();
				e.stopPropagation();
				return;
			} else if(columnUri == "BlankNode" || columnCategory == "temporary") {
				var links = D3ModelManager.getInstance().getCurrentLinksToNode(worksheetId, columnId);
				$.each(links, function(index, link) {
					if(link.target.type == "ColumnNode") {
						//Set Semantic Type
						var type = {
							"uri": link.uri,
							"label": link.label,
							"source": {"uri": target.data('uri'), "id": target.data('id'), "label": target.text()}
						}
						setSemanticType(worksheetId, link.target.id, type);
					} else {
						//Change Links Command
						var newEdges = [];
						var edge = {
							"uri": link.uri,
							"label": link.label,
							"target": link.target,
							"source": {"uri": target.data('uri'), "id": target.data('id'), "label": target.text()}
						}
						newEdges.push(edge);
						changeLinks(worksheetId, alignmentId, [], newEdges);
					}
				});

			} else {
				uri = target.data('uri');
				id = target.data('id');
				console.log("Change Node:" + id + ", " + uri);

				var info = generateInfoObject(worksheetId, "", "ChangeNodeCommand");
				var newInfo = info['newInfo'];
				newInfo.push(getParamObject("alignmentId", alignmentId, "other"));
				newInfo.push(getParamObject("oldNodeId", columnId, "other"));
				newInfo.push(getParamObject("newNodeUri", uri, "other"));
				newInfo.push(getParamObject("newNodeId", id, "other"));
				info["newInfo"] = JSON.stringify(newInfo);
				showLoading(worksheetId);
				var returned = sendRequest(info, worksheetId);
			}
			hide();
		}

		function populateAll() {
			if(allClassCache == null) {
				window.setTimeout(populateAll, 10);
				return;
			}

			var allTypes = [];
			
			$.each(allClassCache, function(index, type) {
				allTypes.push({"label": type["label"], "uri": type["uri"], "id": type["id"]});
			});

			renderMenu($("#class_all", dialog), allTypes);
		}

		function populateRecommended() {
			var inTypes = getClassesInModel(worksheetId);
			var items = [];
			if(inTypes != null) {
				$.each(inTypes, function(index, type) {
					items.push({"label": type["label"], "uri": type["uri"], "id": type["id"], "class": "propertyDropdown_compatible"});
				});	
			}
			renderMenu($("#class_recommended", dialog), items);
		}

		function populateCompatible() {
			var inTypes = getClassesInModel(worksheetId);
			var items = [];
			if(inTypes != null) {
				$.each(inTypes, function(index, type) {
					items.push({"label": type["label"], "uri": type["uri"], "id": type["id"], "class": "propertyDropdown_compatible"});
				});	
			}
			renderMenu($("#class_compatible", dialog), items);
		}

		function filterDropdown(e) {
			query = $("input", dialog).val();
			switch(e.keyCode) {
		        case 40: // down arrow
		        case 38: // up arrow
		        case 16: // shift
		        case 17: // ctrl
		        case 18: // alt
		          break;

		        case 9: // tab
		        case 13: // enter
		          if (!this.shown) return;
		          // this.select();
		          break;

		        case 27: // escape
		          hide();
		          break;
		        default:
		          	items = allClassCache;
		          	items = $.grep(items, function (item) {
			        	return (item["label"].toLowerCase().indexOf(query.toLowerCase()) != -1);
			      	});
			      	renderMenu($("#class_all", dialog), items);
		      }
		}

		function populateMenu() {
			populateRecommended();
			populateCompatible();
			populateAll();
		}

		function renderMenu(div, menuItems) {
			var ul = $("ul", div);
			ul.empty();
			ul.scrollTop(1);

			$.each(menuItems, function(index, item) {
				var label = item["label"];
				var uri = item["uri"];

				var li = $("<li>").addClass("col-xs-4")
				if(label == "divider") {
					li.addClass("divider");
					
				} else {
					
					var a = $("<a>")
						.attr("href", "#")
						.attr("tabindex", "-1")
						.text(label)
						.data('uri', uri)
						.data("id", item["id"])
						.click(selectFromMenu);
					li.append(a);
				}
				if(item["class"])
					li.addClass(item["class"]);
				ul.append(li);
			});

		}


		function show(p_worksheetId, p_columnId, p_columnLabel, p_columnUri, p_columnDomain, p_columnCategory, 
				p_alignmentId, p_nodeType, p_isUri,
				event) {
			worksheetId = p_worksheetId;
			columnLabel = p_columnLabel;
			columnId = p_columnId;
			columnUri = p_columnUri;
			columnDomain = p_columnDomain;
			columnCategory = p_columnCategory;
			alignmentId = p_alignmentId;
			nodeType = p_nodeType;
			isUri = p_isUri;

			$("input", dialog).val('');
			populateMenu();

			$("#classDialog_title", dialog).html("Change Class: " + columnLabel);
			if(columnCategory != "temporary") {
				$("#classDialogFunctions", dialog).show();
				ClassFunctions.getInstance().show(p_worksheetId, p_columnId, p_columnLabel, p_columnUri, p_columnDomain, p_columnCategory, 
													p_alignmentId, p_nodeType, p_isUri, hide, 
													event);
				$("#classDialogSuggestions").removeClass("col-sm-12").addClass("col-sm-10");
			} else {
				$("#classDialogFunctions", dialog).hide();
				$("#classDialogSuggestions").removeClass("col-sm-10").addClass("col-sm-12");
			}

			dialog.modal({
				keyboard: true,
				show: true,
				backdrop: 'static'
			});
		};


		return { //Return back the public methods
			show: show,
			init: init
		};
	};

	function getInstance() {
		if (!instance) {
			instance = new PrivateConstructor();
			instance.init();
		}
		return instance;
	}

	return {
		getInstance: getInstance
	};


})();