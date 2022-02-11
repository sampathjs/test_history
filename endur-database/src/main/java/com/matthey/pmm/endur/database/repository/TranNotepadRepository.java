package com.matthey.pmm.endur.database.repository;

import com.matthey.pmm.endur.database.model.AbTran;
import com.matthey.pmm.endur.database.model.TranNotepad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranNotepadRepository extends JpaRepository<TranNotepad, AbTran> {
}
