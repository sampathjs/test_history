<#setting number_format=",##0">
<head>
    <style>
        table, tr, td {
            border-collapse: collapse;
            border: 1px solid black;
        }

        td {
            padding-left: 0.5em;
            padding-right: 0.5em;
        }

        .details {
            margin-left: 4em;
            margin-bottom: 3em;
        }
    </style>
    <title>breach notification</title>
</head>
<body>
<p>Hi All,</p>
<p></p>
<p>The current breaches for ${runDate} are:</p>
<p></p>
<div class="details">
    <table>
        <tr>
            <td>Run Date/Time</td>
            <td>Run Type</td>
            <td>Desk</td>
            <td>Metal</td>
            <td>Limit (TOz)</td>
            <td>Breach (TOz)</td>
            <td>Breach (£)</td>
            <td>Number of Days in Breach</td>
            <td>Dates of Breach</td>
        </tr>
        <#list results as result>
            <#if result.breach>
                <tr>
                    <#if result.runType == 'Intraday Desk'>
                        <td>${result.runTime.toString("dd-MM-yy HH:mm")}</td>
                    <#else>
                        <td>${result.runTime.toString(datePattern)}</td>
                    </#if>
                    <td>${result.runType}</td>
                    <td>${result.desk}</td>
                    <td>${result.metal}</td>
                    <td>${result.positionLimit}</td>
                    <td>${result.breachTOz}</td>
                    <td>${result.breachGBP}</td>
                    <td>${result.breachDates?size}</td>
                    <td>${result.breachDates?join(dateSeparator)}</td>
                </tr>
            </#if>
        </#list>
    </table>
</div>
<#include "footer.ftl">
</body>
