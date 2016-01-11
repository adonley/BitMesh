var data = {
    labels: ["8/21", "8/22", "8/23", "8/24", "8/25", "8/26", "8/27"],
    datasets: [
        {
            label: "My First dataset",
            fillColor: "rgba(255,255,255,0)",
            strokeColor: "rgba(244,167,36,1)",
            pointColor: "rgba(255,255,255,1)",
            pointStrokeColor: "rgba(244,167,36,1)",
            pointHighlightFill: "rgba(244,167,36,1)",
            pointHighlightStroke: "rgba(244,167,36,1)",
            data: [65, 59, 80, 81, 56, 55, 40]
        },
        {
            label: "My Second dataset",
            fillColor: "rgba(255,255,255,0)",
            strokeColor: "rgba(16,183,184,1)",
            pointColor: "rgba(255,255,255,1)",
            pointStrokeColor: "rgba(16,183,184,1)",
            pointHighlightFill: "rgba(16,183,184,1)",
            pointHighlightStroke: "rgba(16,183,184,1)",
            data: [28, 48, 40, 19, 86, 27, 90]
        }
    ]
};

var options = {
    //String - Colour of the grid lines
    scaleGridLineColor : "#F1F2F7",
    //Number - Tension of the bezier curve between points
    bezierCurveTension : 0.4,
    //Boolean - Whether the line is curved between points
    bezierCurve : false,
    //Number - Pixel width of point dot stroke
    pointDotStrokeWidth : 2,
    // Boolean - whether or not the chart should be responsive and resize when the browser does.
    responsive: true,
    // Boolean - whether to maintain the starting aspect ratio or not when responsive, if set to false, will take up entire container
    maintainAspectRatio: true
};

var context = document.getElementById('main_chart').getContext('2d');
var myLineChart = new Chart(context).Line(data, options);

function updChart() {
    var context = document.getElementById('main_chart').getContext('2d');
    var myLineChart = new Chart(context).Line(data, options);
};

function time() {
    window.setTimeout(updChart, 300);
}

$(window).resize(updChart);
$(document).ready(time);
