$.ajax({
    url: '/api/v1/list_logs',
    dataType: 'json',
    success: function (data) {
        for (var i = 0; i < data.length; i++) {
            var date = new Date(parseInt(data[i].date) * 1000)
            var row = '<tr><td class="column1">' + data[i].id + '</td><td class="column2">' + date.toLocaleString() + '</td><td class="column3">' + data[i].size + '</td><td class="column4"><ul>';
            var subDir = '/files/' + data[i].id + "/" + data[i].id;
            if (data[i].doesSheetExist) {
                row += '<li><a href="' + subDir + '.xlsx" download title="Download Excel Sheet"><span class="material-icons-outlined">download</span></a></li>';
            }
            row += '<li><a href="' + subDir + '.zip" download title="Download All as ZIP"><span class="material-icons-outlined">folder_zip</span></button></a></li>';
            row += '<li><a href="' + subDir + '.txt" target="_blank" title="View Log"><span class="material-icons-outlined">launch</span></a></li>';
            row += '</ul></td></tr>';
            $('#table').append($(row));
        }
    },
    error: function () {
        console.error('Failed to load logs from the API. Make sure the backend is running and has no errors in its console')
    }
});
