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
<p>The following breaches have occurred during the past week period:</p>
<p></p>
<div class="details">
    <#list breachGroups as breachGroup>
        <p style='font-weight: bold'>${breachGroup.breachType}</p>
        <#if breachGroup.breachType?starts_with("Liquidity")>
            <table>
                <tr>
                    <td>Run Date</td>
                    <td>Lower Limit (%)</td>
                    <td>Upper Limit (%)</td>
                    <td>Current Position (%)</td>
                    <td>Breach (%)</td>
                    <td>Breach (TOz)</td>
                    <td>Breach (£)</td>
                    <td>Number of Days in Breach</td>
                </tr>
                <#list breachGroup.breaches as breach>
                    <tr>
                        <td>${breach.runTime.toString(datePattern)}</td>
                        <td>${breach.liquidityLowerLimit}</td>
                        <td>${breach.liquidityUpperLimit}</td>
                        <td>${breach.currentPosition}</td>
                        <td>${breach.liquidityDiff}</td>
                        <td>${breach.breachTOz}</td>
                        <td>${breach.breachGBP}</td>
                        <td>${breach.breachDates?size}</td>
                    </tr>
                </#list>
            </table>
        <#else>
            <table>
                <tr>
                    <#if breachGroup.breachType?contains("Intraday Desk")>
                        <td>Run Time</td>
                    <#else>
                        <td>Run Date</td>
                    </#if>
                    <#if breachGroup.breachType?contains("Desk")>
                        <td>Limit (TOz)</td>
                        <td>Breach (TOz)</td>
                    <#else>
                        <td>Limit (£)</td>
                    </#if>
                    <td>Breach (£)</td>
                    <td>Number of Days in Breach</td>
                </tr>
                <#list breachGroup.breaches as breach>
                    <tr>
                        <#if breachGroup.breachType?contains("Intraday Desk")>
                            <td>${breach.runTime.toString("dd-MM-yy HH:mm")}</td>
                        <#else>
                            <td>${breach.runTime.toString(datePattern)}</td>
                        </#if>
                        <td>${breach.positionLimit}</td>
                        <#if breachGroup.breachType?contains("Desk")>
                            <td>${breach.breachTOz}</td>
                        </#if>
                        <td>${breach.breachGBP}</td>
                        <td>${breach.breachDates?size}</td>
                    </tr>
                </#list>
            </table>
        </#if>
    </#list>
</div>
<#include "footer.ftl">
</body>
