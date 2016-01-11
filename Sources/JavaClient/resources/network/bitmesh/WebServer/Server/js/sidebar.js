$("#menu-toggle").click(function(e) {
    e.preventDefault();
    $("#wrapper").toggleClass("toggled");
    $("#container-fluid").toggleClass("menu-visible");
});

function adjustContainer() {
	var w = window.innerWidth;
	if (w > 768) {
    	$("#container-fluid").removeClass("menu-visible");
    } else {
    	$("#container-fluid").addClass("menu-visible");
    }
};

$(window).resize(adjustContainer);

$(document).ready(adjustContainer);
