package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableOrderCommentTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;

public enum TestOrderComment {
	    // comment IDs have to be synchronised with the automated ID assignment by JPA
		TEST_COMMENT_1 (1000000, "Comment 1, Comment 1, Comment 1, Comment 1, Comment 1,\n\n\n\n Comment 1, Comment 1, Comment 1",
				"2000-01-02 16:00:00", TestUser.JACOB_SMITH, "2001-05-23 12:24:05", TestUser.NIVEDITH_SAJJA),
		TEST_COMMENT_2 (1000001, "Another long comment with line breaks following\n\n\n\nAfter the line breaks",
				"2000-01-02 16:00:00", TestUser.DENNIS_WILDISH, "2000-01-02 16:00:00", TestUser.DENNIS_WILDISH),
		TEST_COMMENT_3 (1000002, "Another Comment having ID 1000003",
				"2005-11-02 16:00:00", TestUser.MURALI_KRISHNAN, "2012-03-23 09:31:45", TestUser.JENS_WAECHTER),
	;
	
	private OrderCommentTo comment;
	
	private TestOrderComment (long id, String commentText, String createdAt, TestUser createdBy,
			String lastUpdate, TestUser updatedBy) {
		comment = ImmutableOrderCommentTo.builder()
				.id(id)
				.commentText(commentText)				
				.createdAt(createdAt)
				.idCreatedByUser(createdBy.getEntity().id())
				.lastUpdate(lastUpdate)
				.idUpdatedByUser(updatedBy.getEntity().id())
				.build();
	}

	public OrderCommentTo getEntity () {
		return comment;
	}
	
	public void setEntity (OrderCommentTo comment) {
		this.comment = comment;
	}
	
	
	public static List<OrderCommentTo> asList () {
		return Arrays.asList(TestOrderComment.values())
				.stream().map(TestOrderComment::getEntity).collect(Collectors.toList());
	}

	public static List<TestOrderComment> asEnumList () {
		return Arrays.asList(TestOrderComment.values())
				.stream().collect(Collectors.toList());
	}
	
	public static Optional<OrderCommentTo> findById(int refId) {
		List<OrderCommentTo> filtered = asList().stream().filter(x -> x.id() == refId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}
}