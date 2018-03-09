<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="xml"/>

<xsl:template match="/">
<xsl:text>&#xa;</xsl:text>
  <trades>
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
	    	
			<xsl:when test="name(.) = 'internal_portfolio'">
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
			
			<xsl:when test="name(.) = 'jm_transaction_id'">
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
			
			<xsl:when test="name(.) = 'event_date'"> <!-- profile CASH-->
				<xsl:element name="valueDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'maturity_date'"> <!-- profile CASH-->
				<xsl:element name="valueDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
	    	
			<xsl:when test="name(.) = 'end_date'"> <!-- profile end date COMM-SWAP-->
				<xsl:element name="valueDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'spot_date'"> <!-- fx_tran_aux_data spot date FX -->
				<xsl:element name="valueDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			
			<xsl:when test="name(.) = 'd_amt555'"> <!-- fx_tran_aux_data -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'para_position112'"> <!-- fx_tran_aux_data -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'd_amt'"> <!-- fx_tran_aux_data -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'c_amt'"> <!-- fx_tran_aux_data -->
				<xsl:element name="toValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			
			<xsl:when test="name(.) = 'para_position111'"> <!-- fx_tran_aux_data -->
				<xsl:element name="toValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'para_position'"> <!-- fx_tran_aux_data -->
				<xsl:element name="toValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

 			<xsl:when test="name(.) = 'd_amt'"> 
				<xsl:element name="dealExcRate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>  
			
			 <xsl:when test="name(.) = 'spot_conv_factor32'"> 
				<xsl:element name="dealExcRate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when> 
			
			<xsl:when test="name(.) = 'ins_type'"> 
				<xsl:element name="ins_type">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when> 

			<xsl:when test="name(.) = 'reference'"> <!-- Deal Comments -->
				<xsl:element name="customerRef">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'tran_status'"> <!-- Status of the deal -->
				<xsl:element name="tradeStatus">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'external_bunit555'"> <!-- Account name -->
				<xsl:element name="metAcName">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'ac_20012'"> <!-- Account info field -->
				<xsl:element name="metAcType">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'account_type'"> <!-- Account info field -->
				<xsl:element name="metAcType">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'iso_code'"> <!-- Country -->
				<xsl:element name="country">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
	    	
	<!-- =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+= -->

	<!--  JDE to OL 4.2.3 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< -->
			<xsl:when test="name(.) = 'proceeds'"> 
				<xsl:element name="toValue">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'para_position87878'"> <!-- Tax currency -->
				<xsl:element name="taxDealCur">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'taxdealCurrency'"> <!-- Tax currency -->
				<xsl:element name="taxDealCur">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'taxdealcurrency'"> <!-- Tax currency -->
				<xsl:element name="taxDealCur">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
		

			<xsl:when test="name(.) = 'te_20002'"> <!-- Base currency of LE-->
				<xsl:element name="taxCode">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'te_20004'"> <!-- Base currency of LE-->
				<xsl:element name="taxCurrency">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'te_20003'"> <!-- Base currency of LE-->
				<xsl:element name="taxRptCur">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
					
			<xsl:when test="name(.) = 'te_20005'"> <!-- Tax FX Rate-->
				<xsl:element name="taxExcRate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'event_date1'"> <!-- profile payment date CASH-->
				<xsl:element name="paymentDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'pymt_date'"> <!-- profile payment date COMM-SWAP-->
				<xsl:element name="paymentDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			
			<xsl:when test="name(.) = 'term_settle_date'"> <!-- fx_tran_aux_date term_settle_date FX-->
				<xsl:element name="paymentDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'ourdocnum'"> <!-- Custom Doc Num -->
				<xsl:element name="invoiceNumber">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'do_20003'"> <!-- Custom Doc Num -->
				<xsl:element name="invoiceNumber">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
						
<!-- 			<xsl:when test="name(.) = 'last_update'"> BO Doc detail
				<xsl:element name="invoiceLine">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
 -->			
			<xsl:when test="name(.) = 'doc_issue_date'"> <!-- BO Doc ??? -->
				<xsl:element name="invoiceDate">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'name'"> <!-- CFlow type -->
				<xsl:element name="billingType">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'pi_20019'"> <!-- Ext LE Info/Group -->
				<xsl:element name="group">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'pi_20020'"> <!-- Ext LE Info/LPPM Member -->
				<xsl:element name="lppmMbr">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>

			<xsl:when test="name(.) = 'pi_20013'"> <!-- Ext LE Info/LBMA Member -->
				<xsl:element name="lbmaMbr">
				  <xsl:value-of select="." />
				 </xsl:element>
			</xsl:when>
	<!-- =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+= -->

	<!--  JDE to OL 4.2.4 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< -->
	
				<xsl:when test="name(.) = 'pi_20035'"> <!--  -->
				<xsl:element name="region">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				

	<!--  JDE to OL 4.2.5 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< -->
	
			
				<xsl:when test="name(.) = 'ins_seq_num'"> <!--  -->
				<xsl:element name="leg">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'fx_flt'"> <!--  -->
				<xsl:element name="fixed_float">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				

				<xsl:when test="name(.) = 'notnl'"> <!-- Ins=COMM SWAP -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'base_amount_toz2'"> <!-- Ins=PREC-EXCH-FUT -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>

				<xsl:when test="name(.) = 'price_unit'"> <!-- Ins=COMM SWAP -->
				<xsl:element name="unitOfMeasure">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>

				<xsl:when test="name(.) = 'base_amount_toz'"> <!-- calculated expression -->
				<xsl:element name="baseWeight">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'd_amt1'"> <!-- base weight = d_amt when FX-->
				<xsl:element name="baseWeight">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'price'"> <!-- Ins=FX & COMM SWAP-->
				<xsl:element name="unitPrice">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'pricefx'"> <!-- Ins=FX & COMM SWAP-->
				<xsl:element name="unitPrice">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'price1swap'"> <!-- Ins=FX & COMM SWAP-->
				<xsl:element name="unitPrice">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'FixedRate'"> <!-- Ins=FX & COMM SWAP-->
				<xsl:element name="unitPrice">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'rate'"> <!-- Ins=FX & COMM SWAP-->
				<xsl:element name="unitPrice">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'total_value1'"> <!-- Ins=COMM SWAP -->
				<xsl:element name="toValue">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'spot_conv_factor55'"> <!-- Ins=FX & SWAP -->
				<xsl:element name="spotEquivPrc">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'rate'"> <!-- Ins=FX & SWAP -->
				<xsl:element name="spotEquivPrc">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'rate1'"> <!-- Ins=FX & SWAP -->
				<xsl:element name="spotEquivPrc">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'price1'"> <!-- Ins=FX & SWAP  & PREC-EXCH-FUT-->
				<xsl:element name="spotEquivPrc">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'spotprice'"> <!-- Ins=FX & SWAP  & PREC-EXCH-FUT-->
				<xsl:element name="spotEquivPrc">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'spot_conv_factor'"> <!-- Ins=LOANDEPOS-->
				<xsl:element name="spotEquivPrc">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'total_value'"> <!-- -->
				<xsl:element name="spotEquivVal">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'pymt'"> <!-- -->
				<xsl:element name="spotEquivVal">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'para_position636363'"> <!-- fx_tran_aux_data -->
				<xsl:element name="spotEquivVal">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'pymt1'"> <!-- -->
				<xsl:element name="spotEquivVal">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'spotequivvalue'"> <!-- FX -->
				<xsl:element name="spotEquivVal">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'proceeds'"> <!-- PREC-EXCH-FUT-->
				<xsl:element name="spotValue">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>

				<xsl:when test="name(.) = 'notnl'"> <!-- Ins=COMM SWAP -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'ti_20015'"> <!-- COMM-SWAP & FX -->
					<xsl:element name="tradingLoc">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'fl_200021'"> <!-- COMM-SWAP & FX -->
					<xsl:element name="tradingLoc">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'external_bunit1'"> <!-- PREC-EXCH-FUT -->
					<xsl:element name="tradingLoc">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'info_value'"> <!-- LME SWAPS - IDX_INFO FIELD ‘LME From CCY’  -->
				<xsl:element name="fromCurrency">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
	    	
				<xsl:when test="name(.) = 'currency'"> <!-- COMM-SWAP & FX -->
					<xsl:element name="fromCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'currency2'"> <!-- PREC-EXCH-FUT -->
					<xsl:element name="fromCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'currency636'"> <!-- LOANS DEPOS -->
					<xsl:element name="fromCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'currencyswap'"> <!-- LMESWAPS -->
					<xsl:element name="toCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'currency6363'"> <!-- LOANS DEPOS -->
					<xsl:element name="toCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'currency1'"> <!-- FX -->
					<xsl:element name="toCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'currency22'"> <!-- PREC-EXCH-FUT -->
					<xsl:element name="toCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>
				<xsl:when test="name(.) = 'settle_ccy'"> <!-- COMM-SWAP -->
					<xsl:element name="toCurrency">
					  <xsl:value-of select="." />
					 </xsl:element>
				</xsl:when>	    	
								
				<xsl:when test="name(.) = 'notnl'"> <!-- COMM-SWAP  -->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>	
				
				<xsl:when test="name(.) = 'base_amount_toz'"> <!-- PREC-EXCH-FUT-->
				<xsl:element name="fromValue">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'unit'"> <!-- ab_tran/unit if FX-->
				<xsl:element name="unitOfMeasure">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>

				<xsl:when test="name(.) = ''"> <!-- JDo e-mail 29-May-2015 -->
					<xsl:element name="taxDealCur">
						<xsl:value-of select="." />
					</xsl:element>
				</xsl:when>	    
				
				<xsl:when test="name(.) = 'ti_20021'"> <!-- -->
				<xsl:element name="isCoverage">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
				<xsl:when test="name(.) = 'ti_20022'"> <!-- -->
				<xsl:element name="coverageText">
				  <xsl:value-of select="." />
				 </xsl:element>
				</xsl:when>
				
	<!-- =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+= -->
			<xsl:when test="name(.) = 'tran_num'">
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