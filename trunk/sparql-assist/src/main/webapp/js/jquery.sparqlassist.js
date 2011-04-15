/**
 * jquery.sparqlassist.js
 * version 0.1
 * Copyright (c) 2007 Luke McCarthy <elmccarthy@gmail.com>, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the Wilkinson Laboratory nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.  THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
$.SparqlAssistant = function($elem, options) {
    // initalize fields...
    this.$elem = $elem;
    this.options = (typeof options == 'object') ? options : {};
    this.namespaces = []; var namespaces = this.namespaces;
    this.documents = []; //var documents = this.documents;
    this.predicates = []; var predicates = this.predicates;
    this.individuals = []; var individuals = this.individuals;
    this.guid = this.options.guid();
    
    // set loading class on element...
    $elem.addClass($.SparqlAssistant.loadingClass);
    $elem.bind({'focus': $.SparqlAssistant.blur});
    
    // load initial data...
    var self = this;
    var loadData = $.SparqlAssistant.loadData;
    var n=3; // JS not actually multithreaded, so this should work
    loadData(options.initNamespaces, namespaces, function (data) {
        for (var i in namespaces) {
            self.options.decorateNamespace(namespaces[i]);
        }
        if (--n <= 0) {
            self.init($elem);
        }
    });
    loadData(options.initPredicates, predicates, function (data) {
        for (var i in predicates) {
            self.options.decoratePredicate(predicates[i]);
        }
        if (--n <= 0) {
            self.init($elem);
        }
    });
    loadData(options.initIndividuals, individuals, function (data) {
        for (var i in individuals) {
            self.options.decorateIndividual(individuals[i]);
        }
        if (--n <= 0) {
            self.init($elem);
        }
    });
};
$.SparqlAssistant.blur = function() {
	$(this).blur();
}; 
// credit to Shahram Javey for the GUID bit
// http://note19.com/2007/05/27/javascript-guid-generator/
$.SparqlAssistant.S4 = function() {
    return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}
$.SparqlAssistant.guid = function() {
    var S4 = $.SparqlAssistant.S4;
    return (S4()+S4()+"-"+S4()+"-"+S4()+"-"+S4()+"-"+S4()+S4()+S4());
}
$.SparqlAssistant.decorateNamespace = function(ns) {
    if (!ns.value && ns.uri) {
        ns.value = ns.label + ": <" + ns.uri + ">\n";
    }
    if (!ns.description && ns.uri) {
    	ns.description = ns.uri;
    }
};
$.SparqlAssistant.decoratePredicate = function(p) {
    if (!p.value && p.uri) {
        p.value = "<" + p.uri + "> ";
    }
    if (!p.label && p.uri) {
    	p.label = p.uri;
    }
};
$.SparqlAssistant.decorateIndividual = function(i) {
    if (!i.value && i.uri) {
        i.value = "<" + i.uri + "> ";
    }
    if (!i.label && i.uri) {
    	i.label = i.uri;
    }
};
$.SparqlAssistant.loadData = function(options, target, callback) {
    if (typeof options == "null" || typeof options == "undefined") { 
        callback();
        return;
    } else if (typeof options == "array") {
        var n = options.length;
        if (n > 0) {
            var arrayCallback = function() { if (--n <= 0) { callback(); } };
            for (var i in options) {
                $.SparqlAssistant.loadData(options[i], target, arrayCallback);
            }
        } else {
            callback();
        }
        return;
    } else if (typeof options == "string") {
        options = {
            url: options
        };
    }
    var o = $.extend({
        dataType : "json",
        complete : callback,
         success : function(data, textStatus, request) {
                       //if (typeof data !== "array") {
                       //    console.log("unexpected data in LoadData");
                       //    console.log(typeof data);
                       //    console.log(data);
                       //    console.log(options);
                       //} else {
                           for (var i=0; i<data.length; ++i) {
                               target.push(data[i]);
                           }
                       //}
                       //callback(data);
                   },
           error : function(request, textStatus, errorThrown) {
                       //console.log("error in LoadData");
                       //console.log(this.url);
                       //console.log(options);
                       //console.log(textStatus);
                       //console.log(errorThrown);
        	           alert("error '" + textStatus + "' loading data from " + this.url);
        	           //callback(data);
                   }
    }, options);
    $.ajax(o);
};
$.SparqlAssistant.loadingClass = "sparqlassist-loading";
$.SparqlAssistant.resultsClass = "sparqlassist-results";
$.SparqlAssistant.prototype.init = function($elem) {
    // create autocompleter on $elem and override some functions...
    $elem.autocomplete({
        displayValue : function(value, data) {
            return data.value;
        },
        showResult : function(value, data) {
            var item = "<p";
            if (data.uri) {
                item += " title='" + data.uri + "'";
            }
            item += "><span class='label'>" + data.label + "</span>";
            if (data.description) {
                item += " <span class='description'>" + data.description + "</span>";
            }
            return item;
        },
        resultsClass : $.SparqlAssistant.resultsClass
    });
    var self = this;
    this.autocompleter = $elem.data('autocompleter');
    this.autocompleter.fetchData = function(value) {
        self.fetchData(value);
    };
    
    // remove loading class on element...
    $elem.removeClass($.SparqlAssistant.loadingClass);
    $elem.unbind({'focus': $.SparqlAssistant.blur});
};
$.SparqlAssistant.prototype.getVariables = function() {
    var sparql = this.$elem.val();
    var variables = [], seen = {};
    var matches = sparql.match(/(\?\S+)/g);
    for (var i in matches) {
        var variable = matches[i];
        if (typeof variable == 'string' && !seen[variable]) {
            variables.push({
                label: variable,
                value: variable + " "
            });
            seen[variable] = true;
        }
    }
    return variables;
};
$.SparqlAssistant.prototype.getPrefixes = function() {
    var sparql = this.$elem.val();
    var prefixes = [], seen = {};
    // note: can't reuse the pattern between the two matches because of 'g';
    //also can't just use one pattern because there's no way to capture with 'g'...
    var matches = sparql.match(/PREFIX\s+([^:]+):\s+\<(.+)\>/gi);
    for (var i in matches) {
        if (typeof matches[i] == 'string') {
            var match = /PREFIX\s+([^:]+):\s+\<(.+)\>/i.exec(matches[i]);
            var prefix = match[1];
            var namespace = match[2];
            if (typeof prefix == 'string' && !seen[prefix]) {
                prefixes.push({
                    label: prefix,
                    value: prefix + ":",
                    description: namespace
                });
                seen[prefix] = true;
            }
        }
    }
    return prefixes;
};
$.SparqlAssistant.prototype.fetchData = function(value) {
    var text = this.$elem.val();
    var caret = this.$elem.caret();
    var precaret = text.substring(0, caret.start);
    var localData, remoteData, decorateData;
    if (precaret.search(/\{/) < 0) {
        // pre-clause syntax...
        var word = precaret.match(/(\w+)\s+\w+$/);
        if (word) {
            word = word[1];
            if (word.toUpperCase() == "PREFIX") {
                localData = this.namespaces;
                remoteData = this.options.remoteNamespaces;
                decorateData = this.options.decorateNamespace;
            } else if (word.toUpperCase() == "SELECT") {
                localData = this.fetchData.SELECT;
            } else if (word.toUpperCase() == "FROM") {
                localData = this.documents;
                remoteData = this.options.remoteDocuments;
                decorateData = this.options.decorateDocument;
            } else if (word.toUpperCase() == "WHERE") {
                localData = this.fetchData.WHERE;
            } else {
                localData = [];
            }
        } else {
            localData = this.fetchData.SYNTAX; 
        }
    } else {
        // inside WHERE clause...
        var clause = precaret.match(/[\{\.]?(.*)$/);
        if (clause) {
            clause = clause[1];
            clause = clause.replace(/^\s+/, '');
            var elem = clause.split(/\s+/);
            if (elem.length == 3) {
                localData = [];
                localData = localData.concat(this.individuals);
                localData = localData.concat(this.getVariables());
                localData = localData.concat(this.getPrefixes());
                remoteData = this.options.remoteIndividuals;
                decorateData = this.options.decorateIndividual;
            } else if (elem.length == 2) {
                localData = this.predicates;
                remoteData = this.options.remotePredicates;
                decorateData = this.options.decoratePredicate;
            } else if (elem.length == 1) {
                localData = [];
                // oh so hacky; fix this when you have time...
                for (var i in this.namespaces) {
                    var ns = this.namespaces[i];
                    localData.push({
                        label : ns.label,
                        value : "<" + ns.uri + ">",
                        description : ns.description,
                        shift : -1
                    });
                }
                localData = localData.concat(this.individuals);
                localData = localData.concat(this.getVariables());
                localData = localData.concat(this.getPrefixes());
                remoteData = this.options.remoteIndividuals;
                decorateData = this.options.decorateIndividual;
            } else {
                // TODO ., OPTIONAL, FILTER, ....
                localData = [];
            }
        }
    }
    var data = [];
    for (var i in localData) {
        var item = localData[i];
        data.push({
            value : item.label,
             data : item
        });
    }
    this.autocompleter.filterAndShowResults(data, value);
    
    // TODO fetch remote data and update the list...
    if (remoteData) {
        this.fetchRemoteData(remoteData, decorateData, value);
    }
};
$.SparqlAssistant.prototype.fetchData.SELECT = [
    { label: "*", value: "*\n" },
    { label: "?", value: "?" }
];
$.SparqlAssistant.prototype.fetchData.WHERE = [
    { label: "{", value: "{\n" }
];
$.SparqlAssistant.prototype.fetchData.SYNTAX = [
    { label: "PREFIX", value: "PREFIX " },
    { label: "FROM", value: "FROM " },
    { label: "SELECT", value: "SELECT " }
];
$.SparqlAssistant.prototype.fetchRemoteData = function(options, decorator, filter) {
    var self = this;
    if (typeof options == "null" || typeof options == "undefined") {
        return;
    } else if (typeof options == 'string') {
        options = {
            url: options
        };
    }
    var o = $.extend({
            data : {
                     query : self.autocompleter.getValue(),
                    sparql : self.$elem.val(),
                     caret : self.$elem.caret().start,
                        id : self.guid
                   },
        dataType : "json",
         success : function(data, textStatus, request) {
                       //if (typeof data !== "array") {
                       //    console.log("unexpected data in LoadData");
                       //    console.log(typeof data);
                       //    console.log(data);
                       //    console.log(options);
                       //} else {
                           var acData = [];
                           for (var i=0; i<data.length; ++i) {
                        	   if (data[i].URI && !data[i].uri) {
                        		   data[i].uri = data[i].URI;
                        	   }
                        	   if (typeof decorator == 'function') {
                        		   decorator(data[i]);
                        	   }
                               acData.push({
                                   value : data[i].label,
                                    data : data[i]
                               });
                           }
                           self.applyRemoteData(self.autocompleter.filterResults(acData, filter));
                       //}
                   },
           error : function(request, textStatus, errorThrown) {
                       console.log("error in LoadData");
                       console.log(this.url);
                       console.log(options);
                       console.log(textStatus);
                       console.log(errorThrown);
                   }
    }, options);
    $.ajax(o);
};
$.SparqlAssistant.prototype.applyRemoteData = function(data) {
    var ac = this.autocompleter;
    var $ul = ac.$ul;
    if (!ac.showing) {
        return;
    }
    $ul.children().each(function() {
        if ($.SparqlAssistant.isInList($(this).data('data'), data)) {
            $(this).addClass("sparqlassist-confirmed");
        } else {
            $(this).addClass("sparqlassist-unconfirmed");
        }
    });
    // this is much harder to read out here, but JSLint suggested it...
    var item, $li;
    var processChild = function() {
        if ($li) {
            // already created it...
            return;
        }
        var data = $(this).data('data');
        var next = $(this).next();
        if (item.data.label > data.label && next) {
            // onto the next element...
            return;
        } else if (item.data.label == data.label && item.data.uri == data.uri) {
        	$(this).addClass("sparqlassist-confirmed");
        }else {
            if (item.data.label && item.data.value) {
                // otherwise no point...
                $li = ac.createListItem(item);
                $li.addClass("sparqlassist-confirmed");
                $(this).before($li);
            }
        }
    };
    for (var i=0; i<data.length; ++i) {
        item = data[i];
//        console.log("adding item");
//        console.log(item);
        $li = null;
        if (ac.showing) {
            $ul.children().each(processChild);
        }
        if (!$li) {
            // list was empty...
            // refactor so this duplicate code is in a local maybeCreateListItem method...
            if (item.data.label && item.data.value) {
                // otherwise no point...
                $li = ac.createListItem(item);
                $li.addClass("sparqlassist-confirmed");
                $ul.append($li);
            }
        }
    }
    if ($ul.children().size() < 1) {
        ac.finish();
    }
};
$.SparqlAssistant.isInList = function(probe, list) {
//    console.log("matching " + probe.label + " / " + probe.value);
    for (var i=0; i<list.length; ++i) {
        var item = list[i].data;
//        console.log("matching against " + item.label + " / " + item.value);
        if (probe.label == item.label &&
            probe.value == item.value) {
//            console.log("found " + probe.label + " in list");
            return list.splice(i, 1);
        } else if (probe.uri && item.uri && probe.uri == item.uri) {
            return true; // it's there, but don't remove it...
        }
    }
//    console.log(probe.label + " is not in list");
//    console.log(list);
    return false;
};
// sparqlassist plugin...
$.fn.sparqlassist = function(options) {
    var o = $.extend({}, $.fn.sparqlassist.defaults, options);
    /* TODO this is really inefficient if they actually want multiple
     * elements to have the same configuration; instead, find a way
     * to load the data just once, then clone it into each instance...
     */
    return this.each(function() {
        var $this = $(this);
        var sa = new $.SparqlAssistant($this, o);
        $this.data('sparqlAssistant', sa);
    });
};
$.fn.sparqlassist.defaults = {
    decorateNamespace  : $.SparqlAssistant.decorateNamespace,
    decoratePredicate  : $.SparqlAssistant.decoratePredicate,
    decorateIndividual : $.SparqlAssistant.decorateIndividual,
//    initNamspaces : ...,
//    initPredicates : ...,
//    initIndividuals : ...,
//    remoteNamspaces : ...,
//    remotePredicates : ...,
//    remoteIndividuals : ...,
    guid               : $.SparqlAssistant.guid
};