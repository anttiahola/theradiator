
(function worker() {
  $.ajax({
    url: 'fragment', 
    success: function(data) {
      $('#content').html(data);
    },
    error: function(jqXhr, textStatus, errorThrown) {
      $('#content').html("<div class='" + textStatus + "'>" + textStatus + ": " + errorThrown + "</div>");
    },
    complete: function() {
      // Schedule the next request when the current one's complete
      setTimeout(worker, 60000);
    }
  });
})();
