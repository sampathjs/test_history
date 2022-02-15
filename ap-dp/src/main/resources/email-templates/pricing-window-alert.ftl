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
    <title>AP DP Pricing Window Alert</title>
</head>
<body>
<p>Hi All,</p>
<p></p>
<p>The following deals are approaching or past agreed pricing/delivery window for ${runDate}:</p>
<p></p>
<div class="details">
    <table>
        <tr>
            <td>Deal</td>
            <td>Type</td>
            <td>Customer</td>
            <td>Deal Date</td>
            <td>Expiry Date</td>
            <td>Days to Expiry</td>
            <td>Open TOz</td>
        </tr>
        <#list results as result>
                <tr>
                    <td>${result.dealNum()}</td>
                    <td>${result.pricingType()}</td>
                    <td>${result.customer()}</td>
                    <td>${result.dealDate()}</td>
                    <td>${result.expiryDate()}</td>
                    <td>${result.numOfDaysToExpiry()}</td>
                    <td>${result.unmatchedVolume()}</td>
                </tr>
        </#list>
    </table>
</div>
<p>Automated system generated alert on ${timestamp}.</p>
</body>
