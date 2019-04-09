$(document).on('click', 'a', function(event){
	/*
		check if the clicked link is an external one
		If it is an external link then open it into a new tab
	*/
	var aminoHost = window.location.host;
	var targetHost = document.createElement("a");
	targetHost.href = event.target;
	if(aminoHost != targetHost){
		event.preventDefault();
		window.open(event.target, '_blank')
	}
});