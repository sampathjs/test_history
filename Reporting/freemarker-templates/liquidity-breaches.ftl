<#include "common-styling.ftl">
<body>
<p>Hi All,</p>
<p></p>
<p>The current liquidity breaches for ${runDate} are:</p>
<p></p>
<div class="details">
    <#list results as result>
        <#if result.breach>
            <p style='font-weight: bold'>${result.metal}</p>
            <table>
                <tr>
                    <td class="title-cell">Dates of Breach(${result.breachDates?size})</td>
                    <#if result.criticalBreachDates>
                        <td class="highlighted">${result.breachDates?join(dateSeparator)}</td>
                    <#else>
                        <td class="content-cell">${result.breachDates?join(dateSeparator)}</td>
                    </#if>
                </tr>
                <tr>
                    <td class="title-cell">Current Position (%)</td>
                    <td class="content-cell">${result.currentPosition?string}</td>
                </tr>
                <tr>
                    <td class="title-cell">Lower Limit (%)</td>
                    <td class="content-cell">${result.liquidityLowerLimit?string}</td>
                </tr>
                <tr>
                    <td>Upper Limit (%)</td>
                    <td class="content-cell">${result.liquidityUpperLimit?string}</td>
                </tr>
                <tr>
                    <td class="title-cell">Breach (%)</td>
                    <td class="content-cell">${result.liquidityDiff?string}</td>
                </tr>
                <tr>
                    <td class="title-cell">Breach (TOz)</td>
                    <td class="content-cell">${result.breachTOz?string}</td>
                </tr>
                <tr>
                    <td class="title-cell">Breach (£)</td>
                    <#if result.critical>
                        <td class="highlighted">${result.breachGBP?string}</td>
                    <#else>
                        <td class="content-cell">${result.breachGBP?string}</td>
                    </#if>
                </tr>
            </table>
        </#if>
    </#list>
</div>
<#include "footer.ftl">
</body>
