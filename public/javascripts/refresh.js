
(function worker() {
  $.ajax({
    url: '/fragment', 
    success: function(data) {
      $('#content').html(data);
    },
    complete: function() {
      // Schedule the next request when the current one's complete
      setTimeout(worker, 60000);
    }
  });
})();
