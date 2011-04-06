/* jQuery Caret by CuDi
 */
(function($) {
	$.extend($.fn, {
		caret: function (start, end) {
			var elem = this[0];
			if (elem) {
				// get caret range
				if (typeof start == "undefined") {
					var docObj = elem.ownerDocument;
					var result = {start:0, end:0};

					if (navigator.appVersion.indexOf("MSIE")!=-1) {
						if (elem.tagName.toLowerCase() == "textarea") {
							if (elem.value.charCodeAt(elem.value.length-1) < 14) {
								elem.value=elem.value.replace(/34/g,'')+String.fromCharCode(28);
							}
							var oRng = docObj.selection.createRange();
							var oRng2 = oRng.duplicate();
							oRng2.moveToElementText(elem);
							oRng2.setEndPoint('StartToEnd', oRng);
							end = elem.value.length-oRng2.text.length;
							oRng2.setEndPoint('StartToStart', oRng);
							start = elem.value.length-oRng2.text.length; 
							if (elem.value.substr(elem.value.length-1) == String.fromCharCode(28)) {
								elem.value = elem.value.substr(0, elem.value.length-1);
							}			
						} else {
							var range = docObj.selection.createRange();
							var r2 = range.duplicate();			
							start = 0 - r2.moveStart('character', -100000);
							end = start + range.text.length;
						}			
					} else {
						start = elem.selectionStart;
						end = elem.selectionEnd;
					}
					if (result.start < 0) {
						result = {start:0, end:0};
					}
				}

				// set caret range
				else {
					var val = this.val();

					if (typeof start != "number") start = -1;
					if (typeof end != "number") end = -1;
					if (start < 0) start = 0;
					if (end > val.length) end = val.length;
					if (end < start) end = start;
					if (start > end) start = end;

					elem.focus();

					if (elem.selectionStart) {
						elem.selectionStart = start;
						elem.selectionEnd = end;
					} else if (document.selection) {
						var range = elem.createTextRange();
						range.collapse(true);
						range.moveStart("character", start);
						range.moveEnd("character", end - start);
						range.select();
					}
				}

				return {start:start, end:end};
			}
		}
	});
})(jQuery);