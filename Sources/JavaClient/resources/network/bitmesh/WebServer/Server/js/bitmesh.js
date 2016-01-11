$(document).ready(function() {

    $("#slider").slider({
        range: "min",
        value: 1,
        step: 1000,
        min: 0,
        max: 5000000,
        slide: function( event, ui ) {
            $( "#price_input" ).val( ui.value + " BTC");
        }
    });

    $("#price_input").val("0 BTC");

    $("#price_input").change(function () {
        var value = this.value.substring(1);
        console.log(value);
        $("#slider").slider("value", parseInt(value));
    });

    /* Display left nav links when clicked */

    function setPageState(page) {
        $("#setup").hide();
        $("#data").hide();
        $("#about").hide();
        $("#contact").hide();
        $("#"+page).show();
        console.log(page);
    };

    setPageState("setup");

    $("#menu_setup").on("click", function() {
        setPageState("setup");
    });
    $("#menu_data").on("click", function() {
        setPageState("data");
    });
    $("#menu_about").on("click", function() {
        setPageState("about");
    });
    $("#menu_contact").on("click", function() {
        setPageState("contact");
    });

});