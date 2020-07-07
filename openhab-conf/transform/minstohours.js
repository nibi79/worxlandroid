/*
Javascript transform function to change the number
of minutes of CPU time from the System Info Binding
into a more readable format
eg: 2365 into '1 day 15 hours 25 minutes

The item in the items file is defined as follow:
Number LocalComputer_Cpu_SystemUptime "[JS(CPUTime.js):%s]"
and linked via PaperUI to the System uptime channel
of the System Info Thing
*/

(function(i) {
    if (i == 'NULL') { return i; }
    if (i == '-') { return 'Undefined'; }
    var val = parseInt(i); // The value sent by OH is a string so we parse into an integer
    var days = 0; // Initialise variables
    var hours = 0;
    var minutes = 0;
    if (val >= 1440) { // 1440 minutes in a days
        days = Math.floor(val / 1440); // Number of days
        val = val - (days * 1440); // Remove days from val
    }
    if (val >= 60) { // 60 minutes in an hour
       hours = Math.floor(val /60); // Number of hours
        val = val - (hours * 60); // Remove hours from val
    }
    minutes = Math.floor(val); // Number of minutes

    var stringDays = ''; // Initialse string variables
    var stringHours = '';
    var stringMinutes = '';
    if (days === 1) {
        stringDays = '1 day '; // Only 1 day so no 's'
    } else if (days > 1) {
        stringDays = days + ' days '; // More than 1 day so 's'
    } // If days = 0 then stringDays remains ''

    if (hours === 1) {
        stringHours = '1 hour '; // Only 1 hour so no 's'
    } else if (hours > 1) {
        stringHours = hours + ' hours '; // More than 1 hour so 's'
    } // If hours = 0 then stringHours remains ''

    if (minutes === 1) {
        stringMinutes = '1 minute'; // Only 1 minute so no 's'
    } else if (minutes > 1) {
        stringMinutes = minutes + ' minutes'; // More than 1 minute so 's'
    } // If minutes = 0 then stringMinutes remains ''

    var returnString =  stringDays + stringHours + stringMinutes
    return returnString.trim(); // Removes the extraneous space at the end

})(input)