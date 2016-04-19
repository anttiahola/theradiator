_charts = [];

function createChart(data, chartNumber, scaleLabel) {
  var chart = _charts[chartNumber]
  if (chart != null) {
	  chart.destroy();
  }
  var ctx = document.getElementById("chart"+chartNumber).getContext("2d");
  _charts[chartNumber] = new Chart(ctx).Line(data, {'scaleLabel': scaleLabel, 'scaleGridLineColor': 'rgba(100, 100, 100, .3)'});
}

Chart.defaults.global.animation = false;
Chart.defaults.global.scaleFontColor = "#AAA";
Chart.defaults.global.responsive = true;
Chart.defaults.global.tooltipTemplate = "<%if (label){%><%=label%>: <%}%><%= value %>";
Chart.defaults.global.scaleLabel = "<%=value%>";
Chart.defaults.global.multiTooltipTemplate = "<%= datasetLabel %>: <%= value %>";
Chart.defaults.global.legendTemplate = "<ul class=\"<%=name.toLowerCase()%>-legend\"><% for (var i=0; i<datasets.length; i++){%><li><span style=\"background-color:<%=datasets[i].fillColor%>\"></span><%if(datasets[i].label){%><%=datasets[i].label%><%}%></li><%}%></ul>";
Chart.defaults.global.scaleGridLineColor = "rgba(255,255,255,.10)";

/**
 * @param chartId 1 or 2
 * @param events The events parameter, remember to escape spaces.
 */
function loadEventsToChart(chartId, events) {
  $.ajax({
    url: '/mp/data/' + encodeURIComponent(events),
    success: function(data) {
      createChart(data, chartId, "<%=value%>");
      $('.content' + chartId + ' h1').text(events);
    },
    error: function(jqXhr, textStatus, errorThrown) {
      $('#messages').html("<div class='" + textStatus + "'>" + textStatus + ": " + errorThrown + "</div>");
    }
  });
}

function loadData() {
  $('#messages').html("");
  loadEventsToChart(1, "results,login");
  loadEventsToChart(2, "Result Sharing Initiated,Results Sharing Completed,Results Share Viewed");
}

function onHashChange() {
  var hash = location.hash;
  var event = hash.substring(1);
  loadData(event);
}



$( document ).ready(function(){
  (function worker() {
    loadData();
    setTimeout(worker, 60000);
  })();
});
