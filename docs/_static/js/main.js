$(document).on('click', 'a', function(event){
	/*
		check if the clicked link is an external one
		If it is an external link then open it into a new tab
	*/
	var aminoHost = window.location.host;
	var target = document.createElement("a");
	target.href = event.target;
	var targetHost = target.href.split('/')[2];
	if(aminoHost != targetHost){
		event.preventDefault();
		window.open(event.target, '_blank')
	}
});