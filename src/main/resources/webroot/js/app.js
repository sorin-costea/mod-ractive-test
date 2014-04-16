var eb = new vertx.EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port
	+ '/eventbus');
var pa = 'vertx.mongopersistor';

var ractive, converter;
var sampleComments = [ {
    author : 'Didi',
    text : 'Sample...'
} ];

// make a markdown converter using the Showdown library -
// https://github.com/coreyti/showdown
converter = new Showdown.converter();

eb.onopen = function() {
    eb.send('vertx.mongopersistor', {
	action : 'find',
	collection : 'comments'
    }, function(reply) {
	if (reply.status === 'ok') {
	    var comments = [];
	    for (var i = 0; i < reply.results.length; i++) {
		comments.push(reply.results[i]);
	    }
	    comments.sort(function(a, b) {
		return b.id - a.id
	    });
	    ractive.set('comments', comments);
	} else {
	    console.error('Failed to retrieve comments: ' + reply.message);
	}
    });
}

ractive = new Ractive({
    el : 'example',
    template : '#template',
    noIntro : true, // disable transitions during initial render
    data : {
	comments : sampleComments,
	renderMarkdown : function(md) {
	    return converter.makeHtml(md);
	}
    }
});

ractive.on('post', function(event) {
    var comment;

    // stop the page reloading
    event.original.preventDefault();

    // we can just grab the comment data from the model, since
    // two-way binding is enabled by default
    comment = {
	author : this.get('author'),
	text : this.get('text')
    };

    this.get('comments').unshift(comment);

    // reset the form
    document.activeElement.blur();
    this.set({
	author : '',
	text : ''
    });

    // fire an event so we can (for example) save the comment to a server
    this.fire('new comment', comment);
});

ractive.on('new comment', function(comment) {
    comment.id =  this.get('comments').length;
    
    eb.send('vertx.mongopersistor', {
	action : 'save',
	collection : 'comments',
	document: comment
    }, function(reply) {
	if (reply.status === 'ok') {
	    // whatever
	} else {
	    console.error('Failed to save comment: ' + reply.message);
	    var posn = this.get('comments').indexOf(comment);
	    if(-1 !== posn) {
		this.get('comments').splice(posn,1);
	    }
	}
    });
});
