(function ($) {
    "use strict";


})(jQuery);

$.ajax({
    url: 'data.json',
    dataType: 'json',
    success: function (data) {
        for (var i = 0; i < data.length; i++) {
            var row = $('<tr><td class="column1">' + data[i].id + '</td><td class="column2">' + data[i].date + '</td><td class="column3">' + data[i].size + '</td><td class="column4"><ul>' +
                    '<li><a href="/data.json" download target="_blank" title="Download Excel Sheet"><span class="material-icons-outlined">download</span></a></li>' +
                    '<li><a title="Download All as ZIP"><span class="material-icons-outlined">folder_zip</span></button></a></li>' +
                    '<li><a title="View Log"><span class="material-icons-outlined">launch</span></a></li>'
                    + '</ul></td></tr>');
            $('#table').append(row);
        }
    },
    error: function (jqXHR, textStatus, errorThrown) {
        alert('Error: ' + textStatus + ' - ' + errorThrown);
    }
});
