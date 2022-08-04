/*
 * MIT License
 *
 * Copyright 2022 Illinois Tech Motorsports
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

$.ajax({
    url: '/api/v1/logs',
    dataType: 'json',
    success: function (data) {
        for (const item of data) {
            const date = new Date(parseInt(item.date) * 1000);
            let row = '<tr><td class="column1">' + item.id + '</td><td class="column2">' + date.toLocaleString() + '</td><td class="column3">' + item.size + '</td><td class="column4"><ul>';
            const subDir = '/files/' + item.id + "/" + item.id;
            if (item.doesSheetExist) {
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
