package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.endur.database.model.AbTran;
import com.matthey.pmm.endur.database.model.Party;
import com.matthey.pmm.endur.database.model.Portfolio;
import com.matthey.pmm.endur.database.repository.AbTranRepository;
import com.matthey.pmm.endur.database.repository.PortfolioRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MtPortfolioRepository extends PortfolioRepository {

    @Query("select portfolio from Portfolio portfolio, Party party, PartyPortfolio partyPortfolio where party = :bu " +
            "and partyPortfolio.partyId = party.partyId and portfolio.idNumber = partyPortfolio.portfolioId")
    List<Portfolio> findPortfoliosByBusinessUnit(@Param("bu") Party businessUnit);
}
