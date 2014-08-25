var eb = new vertx.EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port
    + '/eventbus');
var pa = 'vertx.elasticsearch';

var ractive, converter;
var sampleEmployees = [ {
  first_name : 'Didi',
  last_name : 'Sample',
  about : 'Some description...'
} ];

// make a markdown converter using the Showdown library -
// https://github.com/coreyti/showdown
converter = new Showdown.converter();

eb.onopen = function() {
  eb.send(pa, {
    action : 'search',
    _index : 'megacorp',
    _type : 'employee'
  }, function(reply) {
    if (reply.status === 'ok') {
      var employees = [];
      for (var i = 0; i < reply.hits.total; i++) {
        employees.push(reply.hits.hits[i]._source);
      }
      employees.sort(function(a, b) {
        if (0 === a.last_name.localeCompare(b.last_name))
          return a.first_name.localeCompare(b.first_name);
        return a.last_name.localeCompare(b.last_name);
      });
      ractive.set('employees', employees);
    } else {
      console.error('Failed to retrieve employees: ' + reply.message);
    }
  });
}

ractive = new Ractive({
  el : 'example',
  template : '#template',
  noIntro : true, // disable transitions during initial render
  data : {
    employees : sampleEmployees,
    renderMarkdown : function(md) {
      return converter.makeHtml(md);
    }
  }
});

ractive.on('post', function(event) {
  var employee;

  // stop the page reloading
  event.original.preventDefault();

  // we can just grab the employee data from the model, since
  // two-way binding is enabled by default
  employee = {
    first_name : String(this.get('first_name')),
    last_name : String(this.get('last_name')),
    about : String(this.get('about'))
  };

  // assume backend will work, so show already
  var employees = this.get('employees');
  employees.unshift(employee);
  employees.sort(function(a, b) {
    if (0 === a.last_name.localeCompare(b.last_name))
      return a.first_name.localeCompare(b.first_name);
    return a.last_name.localeCompare(b.last_name);
  });

  // reset the form
  document.activeElement.blur();
  this.set({
    first_name : '',
    last_name : '',
    about : ''
  });

  // fire an event so we can (for example) save the employee to a server
  this.fire('new employee', employee);
});

ractive.on('new employee', function(employee) {
  employee.id = String(this.get('employees').length);

  eb.send(pa, {
    action : 'index',
    _index : 'megacorp',
    _type : 'employee',
    _id : employee.id,
    _source : employee
  }, function(reply) {
    if (reply.status === 'ok') {
      // whatever
    } else {
      console.error('Failed to save employee: ' + reply.message);
      // remove it from displayed list, sorry you don't belong there
      var posn = this.get('employees').indexOf(employee);
      if (-1 !== posn) {
        this.get('employees').splice(posn, 1);
      }
    }
  });
});
