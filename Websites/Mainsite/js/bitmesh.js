$(document).ready(function()
{
	#("#faq_text").hide();

	$("#faq_link").click(function(event)
	{
		event.preventDefault();
	});

	$("#faq").click(function(event)
	{
		event.preventDefault();
		$("#faq_text").show();
	});

});