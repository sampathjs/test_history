<#include "common-styling.ftl">
<body>
<p>Hi All,</p>
<p></p>
<p>The current overnight breaches for ${runDate} are:</p>
<p></p>
<div class="details">
    <table>
        <tr>
            <td class="title-cell">Dates of Breach(${result.breachDates?size})</td>
            <td class="content-cell">${result.breachDates?join(dateSeparator)}</td>
        </tr>
        <tr>
            <td class="title-cell">Limit (£)</td>
            <td class="content-cell">${result.positionLimit?string}</td>
        </tr>
        <tr>
            <td class="title-cell">Breach (£)</td>
            <td class="content-cell">${result.breachGBP?string}</td>
        </tr>
    </table>
</div>
<#include "footer.ftl">
</body>
