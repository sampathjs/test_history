<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="xml"/>
    <xsl:template name="stripHead">
        <xsl:param name="in"/>
        <xsl:choose>
            <xsl:when test="starts-with($in, ' ')">
                <xsl:call-template name="stripHead">
                    <xsl:with-param name="in" select="substring($in, 2)"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$in"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="stripTail">
        <xsl:param name="in"/>
        <xsl:choose>
            <xsl:when test="substring($in, string-length($in))=' '">
                <xsl:call-template name="stripTail">
                    <xsl:with-param name="in" select="substring($in, 1, string-length($in) - 1)"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$in"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="strip">
        <xsl:param name="in"/>
        <xsl:call-template name="stripHead">
            <xsl:with-param name="in">
                <xsl:call-template name="stripTail">
                    <xsl:with-param name="in">
                        <xsl:value-of select="$in"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="charToHex">
        <xsl:param name="in"/>
        <xsl:choose>
            <xsl:when test="$in=' '">
                <xsl:value-of select="'20'"/>
            </xsl:when>  
            <xsl:when test="$in='!'">
                <xsl:value-of select="'21'"/>
            </xsl:when>
            <xsl:when test="$in='#'">
                <xsl:value-of select="'23'"/>
            </xsl:when>
            <xsl:when test="$in='$'">
                <xsl:value-of select="'24'"/>
            </xsl:when>
            <xsl:when test="$in='%'">
                <xsl:value-of select="'25'"/>
            </xsl:when>
            <xsl:when test="$in='('">
                <xsl:value-of select="'28'"/>
            </xsl:when>
            <xsl:when test="$in=')'">
                <xsl:value-of select="'29'"/>
            </xsl:when>
            <xsl:when test="$in='*'">
                <xsl:value-of select="'2a'"/>
            </xsl:when>
            <xsl:when test="$in='+'">
                <xsl:value-of select="'2b'"/>
            </xsl:when>
            <xsl:when test="$in=','">
                <xsl:value-of select="'2c'"/>
            </xsl:when>
            <xsl:when test="$in='-'">
                <xsl:value-of select="'2d'"/>
            </xsl:when>
            <xsl:when test="$in='.'">
                <xsl:value-of select="'2e'"/>
            </xsl:when>
            <xsl:when test="$in='/'">
                <xsl:value-of select="'2f'"/>
            </xsl:when>
            <xsl:when test="$in='0'">
                <xsl:value-of select="'30'"/>
            </xsl:when>
            <xsl:when test="$in='1'">
                <xsl:value-of select="'31'"/>
            </xsl:when>
            <xsl:when test="$in='2'">
                <xsl:value-of select="'32'"/>
            </xsl:when>
            <xsl:when test="$in='3'">
                <xsl:value-of select="'33'"/>
            </xsl:when>
            <xsl:when test="$in='4'">
                <xsl:value-of select="'34'"/>
            </xsl:when>
            <xsl:when test="$in='5'">
                <xsl:value-of select="'35'"/>
            </xsl:when>
            <xsl:when test="$in='6'">
                <xsl:value-of select="'36'"/>
            </xsl:when>
            <xsl:when test="$in='7'">
                <xsl:value-of select="'37'"/>
            </xsl:when>
            <xsl:when test="$in='8'">
                <xsl:value-of select="'38'"/>
            </xsl:when>
            <xsl:when test="$in='9'">
                <xsl:value-of select="'39'"/>
            </xsl:when>
            <xsl:when test="$in=':'">
                <xsl:value-of select="'3a'"/>
            </xsl:when>
            <xsl:when test="$in=';'">
                <xsl:value-of select="'3b'"/>
            </xsl:when>
            <xsl:when test="$in='='">
                <xsl:value-of select="'3d'"/>
            </xsl:when>
            <xsl:when test="$in='?'">
                <xsl:value-of select="'3f'"/>
            </xsl:when>
            <xsl:when test="$in='@'">
                <xsl:value-of select="'40'"/>
            </xsl:when>
            <xsl:when test="$in='A'">
                <xsl:value-of select="'41'"/>
            </xsl:when>
            <xsl:when test="$in='B'">
                <xsl:value-of select="'42'"/>
            </xsl:when>
            <xsl:when test="$in='C'">
                <xsl:value-of select="'43'"/>
            </xsl:when>
            <xsl:when test="$in='D'">
                <xsl:value-of select="'44'"/>
            </xsl:when>
            <xsl:when test="$in='E'">
                <xsl:value-of select="'45'"/>
            </xsl:when>
            <xsl:when test="$in='F'">
                <xsl:value-of select="'46'"/>
            </xsl:when>
            <xsl:when test="$in='G'">
                <xsl:value-of select="'47'"/>
            </xsl:when>
            <xsl:when test="$in='H'">
                <xsl:value-of select="'48'"/>
            </xsl:when>
            <xsl:when test="$in='I'">
                <xsl:value-of select="'49'"/>
            </xsl:when>
            <xsl:when test="$in='J'">
                <xsl:value-of select="'4a'"/>
            </xsl:when>
            <xsl:when test="$in='K'">
                <xsl:value-of select="'4b'"/>
            </xsl:when>
            <xsl:when test="$in='L'">
                <xsl:value-of select="'4c'"/>
            </xsl:when>
            <xsl:when test="$in='M'">
                <xsl:value-of select="'4d'"/>
            </xsl:when>
            <xsl:when test="$in='N'">
                <xsl:value-of select="'4e'"/>
            </xsl:when>
            <xsl:when test="$in='O'">
                <xsl:value-of select="'4f'"/>
            </xsl:when>
            <xsl:when test="$in='P'">
                <xsl:value-of select="'50'"/>
            </xsl:when>
            <xsl:when test="$in='Q'">
                <xsl:value-of select="'51'"/>
            </xsl:when>
            <xsl:when test="$in='R'">
                <xsl:value-of select="'52'"/>
            </xsl:when>
            <xsl:when test="$in='S'">
                <xsl:value-of select="'53'"/>
            </xsl:when>
            <xsl:when test="$in='T'">
                <xsl:value-of select="'54'"/>
            </xsl:when>
            <xsl:when test="$in='U'">
                <xsl:value-of select="'55'"/>
            </xsl:when>
            <xsl:when test="$in='V'">
                <xsl:value-of select="'56'"/>
            </xsl:when>
            <xsl:when test="$in='W'">
                <xsl:value-of select="'57'"/>
            </xsl:when>
            <xsl:when test="$in='X'">
                <xsl:value-of select="'58'"/>
            </xsl:when>
            <xsl:when test="$in='Y'">
                <xsl:value-of select="'59'"/>
            </xsl:when>
            <xsl:when test="$in='Z'">
                <xsl:value-of select="'5a'"/>
            </xsl:when>
            <xsl:when test="$in='['">
                <xsl:value-of select="'5b'"/>
            </xsl:when>
            <xsl:when test="$in='\'">
                <xsl:value-of select="'5c'"/>
            </xsl:when>
            <xsl:when test="$in=']'">
                <xsl:value-of select="'5d'"/>
            </xsl:when>
            <xsl:when test="$in='^'">
                <xsl:value-of select="'5e'"/>
            </xsl:when>
            <xsl:when test="$in='_'">
                <xsl:value-of select="'5f'"/>
            </xsl:when>
            <xsl:when test="$in='`'">
                <xsl:value-of select="'60'"/>
            </xsl:when>
            <xsl:when test="$in='a'">
                <xsl:value-of select="'61'"/>
            </xsl:when>
            <xsl:when test="$in='b'">
                <xsl:value-of select="'62'"/>
            </xsl:when>
            <xsl:when test="$in='c'">
                <xsl:value-of select="'63'"/>
            </xsl:when>
            <xsl:when test="$in='d'">
                <xsl:value-of select="'64'"/>
            </xsl:when>
            <xsl:when test="$in='e'">
                <xsl:value-of select="'65'"/>
            </xsl:when>
            <xsl:when test="$in='f'">
                <xsl:value-of select="'66'"/>
            </xsl:when>
            <xsl:when test="$in='g'">
                <xsl:value-of select="'67'"/>
            </xsl:when>
            <xsl:when test="$in='h'">
                <xsl:value-of select="'68'"/>
            </xsl:when>
            <xsl:when test="$in='i'">
                <xsl:value-of select="'69'"/>
            </xsl:when>
            <xsl:when test="$in='j'">
                <xsl:value-of select="'6a'"/>
            </xsl:when>
            <xsl:when test="$in='k'">
                <xsl:value-of select="'6b'"/>
            </xsl:when>
            <xsl:when test="$in='l'">
                <xsl:value-of select="'6c'"/>
            </xsl:when>
            <xsl:when test="$in='m'">
                <xsl:value-of select="'6d'"/>
            </xsl:when>
            <xsl:when test="$in='n'">
                <xsl:value-of select="'6e'"/>
            </xsl:when>
            <xsl:when test="$in='o'">
                <xsl:value-of select="'6f'"/>
            </xsl:when>
            <xsl:when test="$in='p'">
                <xsl:value-of select="'70'"/>
            </xsl:when>
            <xsl:when test="$in='q'">
                <xsl:value-of select="'71'"/>
            </xsl:when>
            <xsl:when test="$in='r'">
                <xsl:value-of select="'72'"/>
            </xsl:when>
            <xsl:when test="$in='s'">
                <xsl:value-of select="'73'"/>
            </xsl:when>
            <xsl:when test="$in='t'">
                <xsl:value-of select="'74'"/>
            </xsl:when>
            <xsl:when test="$in='u'">
                <xsl:value-of select="'75'"/>
            </xsl:when>
            <xsl:when test="$in='v'">
                <xsl:value-of select="'76'"/>
            </xsl:when>
            <xsl:when test="$in='w'">
                <xsl:value-of select="'77'"/>
            </xsl:when>
            <xsl:when test="$in='x'">
                <xsl:value-of select="'78'"/>
            </xsl:when>
            <xsl:when test="$in='y'">
                <xsl:value-of select="'79'"/>
            </xsl:when>
            <xsl:when test="$in='z'">
                <xsl:value-of select="'7a'"/>
            </xsl:when>
            <xsl:when test="$in='{'">
                <xsl:value-of select="'7b'"/>
            </xsl:when>
            <xsl:when test="$in='|'">
                <xsl:value-of select="'7c'"/>
            </xsl:when>
            <xsl:when test="$in='}'">
                <xsl:value-of select="'7d'"/>
            </xsl:when>
            <xsl:when test="$in='~'">
                <xsl:value-of select="'7e'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'xx'"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="stringToHex">
        <xsl:param name="in"/>
        <xsl:choose>
            <xsl:when test="string-length($in)>0">
                <xsl:variable name="head">
                    <xsl:call-template name="charToHex">
                        <xsl:with-param name="in" select="substring($in, 1, 1)"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="tail">
                    <xsl:call-template name="stringToHex">
                        <xsl:with-param name="in" select="substring($in, 2)"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:value-of select="concat($head, $tail)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="''"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="mangle">
        <xsl:param name="url"/>
        <xsl:param name="hint"/>

        <xsl:variable name="norm_url">
            <xsl:call-template name="strip">
                <xsl:with-param name="in" select="$url"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$norm_url='about:blank'">
                <xsl:value-of select="'/+CSCOL+/ab.html'"/>
            </xsl:when>
            <xsl:when test="starts-with($norm_url, 'javascript:')">
                <xsl:value-of select="$norm_url"/>
            </xsl:when>
            <xsl:when test="starts-with($norm_url, '#')">
                <xsl:value-of select="$norm_url"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name='code'>
                    <xsl:call-template name="getCode">
                        <xsl:with-param name="in" select="$norm_url"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$code=3">
                        <xsl:value-of select="concat($norm_url, '-CSCO-', string($code), string($hint), '--')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:variable name="server">
                            <xsl:call-template name="getTargetServer">
                                <xsl:with-param name="in" select="$norm_url"/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:variable name="hex">
                            <xsl:call-template name="stringToHex">
                                <xsl:with-param name="in" select="translate($server, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz', 'NOPQRSTUVWXYZABCDEFGHIJKLMnopqrstuvwxyzabcdefghijklm')"/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:variable name="dstUrl">
                            <xsl:call-template name="getUrl">
                                <xsl:with-param name="in" select="$norm_url"/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:value-of select="concat('', '/+CSCO+', string($code), string($hint), string($hex), '++', string($dstUrl))"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="getCode">
        <xsl:param name="in"/>
        <xsl:choose>
            <xsl:when test="starts-with($in, 'https:/') or starts-with($in, 'http:/')">
                <xsl:value-of select="'0'"/>
            </xsl:when>
            <xsl:when test="starts-with($in, '/')">
                <xsl:value-of select="'1'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'3'"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="getTargetServer">
        <xsl:param name="in"/>
        <xsl:variable name="code">
            <xsl:call-template name="getCode">
                <xsl:with-param name="in" select="$in"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$code=0">
                <xsl:variable name="server" select="substring-after($in, '//')"/>
                <xsl:choose>
                    <xsl:when test="contains($server, '/')">
                        <xsl:value-of select="concat(substring-before($in, '//'), '//', substring-before($server, '/'))"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$in"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="getTargetServer">
                    <xsl:with-param name="in" select="'http://gbroenopl01.metals.johnsonmatthey.com/secure/attachment/12839/'"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="check">
        <xsl:param name="a"/>
        <xsl:param name="b"/>
        <xsl:choose>
            <xsl:when test="$a=$b">
                <xsl:value-of select="'Ok'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'Failed'"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="tableHeader">
        <tr>
            <th>Result</th>
            <th>Src</th>
            <th>Processed</th>
            <th>Expected</th>
        </tr>
    </xsl:template>

    <xsl:template name="getUrl">
        <xsl:param name="in"/>
        <xsl:variable name="code">
            <xsl:call-template name="getCode">
                <xsl:with-param name="in" select="$in"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$code=0">
                <xsl:variable name="server">
                    <xsl:call-template name="getTargetServer">
                        <xsl:with-param name="in" select="$in"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:value-of select="concat('/', substring($in, string-length($server) + 2))"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$in"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


<xsl:template match="/">
<xsl:text>&#xa;</xsl:text>
  <trades>
    <xsl:apply-templates/>
  </trades>
</xsl:template>
 
<!--  general OLF to JDE Metals report transformation --> 
<xsl:template match="Table/Data/$$ReportBuilder_output$$">
	<xsl:element name="trade">
	  <xsl:for-each select="*">
	    <xsl:choose>	<!--  name fields based on JDE Metals interface -->
		
	    	<xsl:when test="name(.) = 'fl_20002'">
	    	  <xsl:element name="deskLocation">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20015'">
	    	  <xsl:element name="businessUnit">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>

			<xsl:when test="name(.) = 'pi_20016'">
	    	  <xsl:element name="lglEntity">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>
			
			<xsl:when test="name(.) = 'external_bunit'">
	    	  <xsl:element name="busUnitDesc">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>
			
			<xsl:when test="name(.) = 'external_lentity'">
	    	  <xsl:element name="lglEntityDesc">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>
			
			<xsl:when test="name(.) = 'pi_20018'">
	    	  <xsl:element name="intLglEnt">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>			

			<xsl:when test="name(.) = 'pi_20017'">
	    	  <xsl:element name="intBunit">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>

			<xsl:when test="name(.) = 'internal_lentity'">
	    	  <xsl:element name="intLglEntDesc">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>

			<xsl:when test="name(.) = 'internal_bunit'">
	    	  <xsl:element name="intBunitDesc">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>			
			

			<xsl:when test="name(.) = 'internal_portfolio'">
	    	  <xsl:element name="portfolio">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>

			<xsl:when test="name(.) = 'ac_20018'">
	    	  <xsl:element name="jmAccNo">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>

			<xsl:when test="name(.) = 'pi_20028'">
	    	  <xsl:element name="pmmAccount">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'ti_20019'">
	    	  <xsl:element name="tradeType">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>			

			<xsl:when test="name(.) = 'deal_tracking_num'">
	    	  <xsl:element name="tradeRef">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'ins_seq_num'">
	    	  <xsl:element name="leg">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'trade_date'">
	    	  <xsl:element name="tradeDate">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'ti_20015'">
	    	  <xsl:element name="tradingLoc">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'currency'">
	    	  <xsl:element name="fromCurrency">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'total_quantity'"> <!-- Form for COMM-PHYS DP & RC --> 
	    	  <xsl:element name="fromValue">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	
			
			<xsl:when test="name(.) = 'para_position'"> <!-- Form for CASH - Cash Transfers TR --> 
	    	  <xsl:element name="fromValue">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'unit'">
	    	  <xsl:element name="unitOfMeasure">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>			

			<xsl:when test="name(.) = 'base_amount_toz'">
	    	  <xsl:element name="baseWeight">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'reference'">
	    	  <xsl:element name="customerRef">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'tran_status'">
	    	  <xsl:element name="tradeStatus">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>		

			<xsl:when test="name(.) = 'location_id'">
	    	  <xsl:element name="site">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'comm_form_name'"> <!-- Form for COMM-PHYS DP & RC --> 
	    	  <xsl:element name="form">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	
			
			<xsl:when test="name(.) = 'ti_20014'"> <!-- Form for CASH - Cash Transfers TR --> 
	    	  <xsl:element name="form">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'upper_value'">
	    	  <xsl:element name="purity">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>		

			<xsl:when test="name(.) = 'external_bunit1'"> <!-- Think we should be using account_id as below --> 
	    	  <xsl:element name="metAcName">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>		
			
			<xsl:when test="name(.) = 'account_id'">
	    	  <xsl:element name="metAcName">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'ac_20012'">
	    	  <xsl:element name="metAcType">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'ins_type'">
	    	  <xsl:element name="ins_type">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	
			

			<xsl:when test="name(.) = 'event_date'">
	    	  <xsl:element name="valueDate">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	

			<xsl:when test="name(.) = 'pi_20019'">
	    	  <xsl:element name="group">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>	
			
			<xsl:when test="name(.) = 'pi_20035'">
	    	  <xsl:element name="region">
			    <xsl:value-of select="."/>
			  </xsl:element>
	    	</xsl:when>						
				
			<xsl:when test="name(.) = 'int_settle_id'"> <!--  -->
				<xsl:element name="intMetAcName">
				  <xsl:value-of select="."/>
				 </xsl:element>
			</xsl:when>
			
			<xsl:when test="name(.) = 'account_number'"> <!--  -->
				<xsl:element name="intPmmAccount">
				  <xsl:value-of select="."/>
				 </xsl:element>
			</xsl:when>
				
	<!-- =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+= -->
			<xsl:when test="name(.) = 'tran_num'">
			  <!-- suppress tran_num from reported results -->
				
			</xsl:when>
			
		    <xsl:otherwise> <!-- catch everything else to just report name and value, so we drop all attributes -->
			  	<xsl:copy>
				  	  <xsl:value-of select="."/>
			  	</xsl:copy>
		    </xsl:otherwise>
	    
	    
	    </xsl:choose>
	  </xsl:for-each>	  
	</xsl:element>
	
	</xsl:template>

</xsl:stylesheet> 