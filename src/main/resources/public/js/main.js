(function () {
    "use strict";

})(jQuery);

$.ajax({
    url: '/api/list_logs',
    dataType: 'json',
    success: function (data) {
        for (var i = 0; i < data.length; i++) {
            var row = '<tr><td class="column1">' + data[i].id + '</td><td class="column2">' + data[i].date + '</td><td class="column3">' + data[i].size + '</td><td class="column4"><ul>';
            if (data[i].sheet !== "") {
                row += '<li><a href="/' + data[i].sheet + '" download title="Download Excel Sheet"><span class="material-icons-outlined">download</span></a></li>';
            }
            if(data[i].zip !== "") {
                row += '<li><a href="/' + data[i].zip + '" download title="Download All as ZIP"><span class="material-icons-outlined">folder_zip</span></button></a></li>';
            }
            if(data[i].log !== "") {
                row += '<li><a href="/' + data[i].log + '" target="_blank" title="View Log"><span class="material-icons-outlined">launch</span></a></li>';
            }
            row += '</ul></td></tr>';
            $('#table').append($(row));
        }
    },
    error: function (jqXHR, textStatus, errorThrown) {
        alert('Error: ' + textStatus + ' - ' + errorThrown);
    }
});
