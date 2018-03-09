<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="xml"/>

<xsl:template match="/">
<xsl:text>&#xa;</xsl:text>
  <trades xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:noNamespaceSchemaLocation="trade_Modified.xsd">
    <xsl:apply-templates/>
  </trades>
</xsl:template>
 
<!--  general OLF to JDE report transformation --> 
<xsl:template match="Table/Data/$$ReportBuilder_output$$">
	<xsl:element name="trade">
	  <xsl:for-each select="*">
	    <xsl:choose>	<!--  name fields based on JDE interface -->
		
	    	<xsl:when test="name(.) = 'fl_20002'">
	    	  <xsl:element name="deskLocation">
			    <xsl:value-of select="." />
			  </xsl:element>
	    	</xsl:when>

	    	<xsl:when test="name(.) = 'pi_20015'">
	    	  <xsl:element name="businessUnit">
			    <xsl:value-of select="." />
			  </xsl:element>
	    	</xsl:when>

	    	<xsl:when test="name(.) = 'pi_20016'">
	    	  <xsl:element name="lglEntity">
			    <xsl:value-of select="." />
			  </xsl:element>
	    	</xsl:when>

	    	<xsl:when test="name(.) = 'external_bunit'">
	    	  <xsl:element name="busUnitDesc">
			    <xsl:value-of select="." />
			  </xsl:element>
	    	</xsl:when>
	    	
	    	<xsl:when test="name(.) = 'external_lentity'">
	    	  <xsl:element name="lglEntityDesc">
			    <xsl:value-of select="." />
			  </xsl:element>
	    	</xsl:when>
				    
			<xsl:when test="name(.) = 'pi_20018'">
				<xsl:element name="intLglEnt">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) ='pi_20017'">
				 <xsl:element name="intBunit">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>	    	

			<xsl:when test="name(.) = 'internal_lentity'">
				<xsl:element name="intLglEntDesc">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'internal_bunit'">
				<xsl:element name="intBunitDesc">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
	    	
			<xsl:when test="name(.) = 'portfolio_id'">
				<xsl:element name="portfolio">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
	    	
	    	<xsl:when test="name(.) = 'ac_20018'">
				<xsl:element name="jmAccNo">
				  <xsl:value-of select="." />
				 </xsl:element>
	    	</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20028'">
				<xsl:element name="pmmAccount">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20030'">
				<xsl:element name="pmmAccount">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20029'">
				<xsl:element name="pmmAccount">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ti_20019'">
				<xsl:element name="tradeType">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'deal_tracking_num'">
				<xsl:element name="tradeRef">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
				    
			<xsl:when test="name(.) = 'trade_date'">
				<xsl:element name="tradeDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ins_para_seq_num'">
				<xsl:element name="leg">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ti_20015'">
				<xsl:element name="tradingLoc">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'tran_currency'">
				<xsl:element name="fromCurrency">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'settle_ccy'">
				<xsl:element name="toCurrency">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'tran_settle_date'">
				<xsl:element name="valueDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'traded_position'"> <!--  -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'tran_unit'">
				<xsl:element name="unitOfMeasure">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'tran_position'">
				<xsl:element name="baseWeight">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ti_20031'">
				<xsl:element name="unitPrice">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'settle_amount'"> <!--  -->
				<xsl:element name="toValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ti_20061'">
				<xsl:element name="dealExcRate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ti_200311'">
				<xsl:element name="spotEquivPrc">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'settle_amount1'">
				<xsl:element name="spotEquivVal">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'para_position'"> <!--   -->
				<xsl:element name="taxDealCur">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
		

			<xsl:when test="name(.) = 'te_20002'"> <!--  -->
				<xsl:element name="taxCode">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'te_20004'"> <!--  -->
				<xsl:element name="taxCurrency">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'te_20003'"> <!--  -->
				<xsl:element name="taxRptCur">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
					
			<xsl:when test="name(.) = 'te_20005'"> <!--  -->
				<xsl:element name="taxExcRate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'tran_reference'"> <!--  -->
				<xsl:element name="customerRef">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pymt_due_date'"> <!--  -->
				<xsl:element name="paymentDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'our_doc_num'"> <!--  -->
				<xsl:element name="invoiceNumber">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'doc_issue_date'"> <!--  -->
				<xsl:element name="invoiceDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'tran_status'"> <!--  -->
				<xsl:element name="tradeStatus">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ti_20021'"> <!--  -->
				<xsl:element name="isCoverage">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ti_20022'"> <!--  -->
				<xsl:element name="coverageText">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'external_bunit1'"> <!--  -->
				<xsl:element name="metAcName">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ac_20012'"> <!--  -->
				<xsl:element name="metAcType">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'iso_code'"> <!--  -->
				<xsl:element name="country">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20019'"> <!--  -->
				<xsl:element name="group">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20035'"> <!--  -->
				<xsl:element name="region">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20013'"> <!--  -->
				<xsl:element name="lppmMbr">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20020'"> <!--  -->
				<xsl:element name="lbmaMbr">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ins_type'"> <!--  -->
				<xsl:element name="ins_type">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'cflow_type'"> <!--  -->
				<xsl:element name="billingType">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'fx_flt'"> <!--  -->
				<xsl:element name="fixed_float">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
					
	<!-- =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+= -->
			<xsl:when test="name(.) = 'tran_num'">
			  <!-- suppress tran_num from reported results -->
				
			</xsl:when>
			
			<xsl:when test="name(.) = 'document_num'">
			  <!-- suppress tran_num from reported results -->
				
			</xsl:when>
			
		    <xsl:otherwise> <!-- catch everything else to just report name and value, so we drop all attributes -->
			  	<xsl:copy>
				  	  <xsl:value-of select="." />
			  	</xsl:copy>
		    </xsl:otherwise>
	    
	    
	    </xsl:choose>
	  </xsl:for-each>	  
	</xsl:element>
	
	</xsl:template>

</xsl:stylesheet> 