package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.mock.MockOrderCommentController;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestOrderComment;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableOrderCommentTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;
import com.matthey.pmm.toms.transport.OrderTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
@Transactional
public class OrderCommentControllerTest {
	@Autowired
	protected MockOrderCommentController commentController;	
	
	@Autowired
	protected OrderCommentRepository commentRepo;

	@Autowired
	protected OrderCommentConverter commentConverter;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;
	
	@Autowired
	protected ReferenceOrderRepository refOrderRepo;
	
	@Autowired
	protected LimitOrderConverter limitOrderConverter;
	
	@Autowired
	protected ReferenceOrderConverter refOrderConverter;

	
	private Map<OrderTo, OrderCommentTo> orderCommentsToDelete;
	private Map<OrderTo, OrderCommentTo> orderCommentsToRestore;	
	
	@Before
	public void init() {
		orderCommentsToDelete = new HashMap<>();
		orderCommentsToRestore = new HashMap<>();
	}
	
	@After
	public void tearDown() {
		for (Map.Entry<OrderTo, OrderCommentTo> entry : orderCommentsToDelete.entrySet()) {
			if (entry.getKey() instanceof LimitOrderTo) {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (limitOrderRepo.existsById(id)) {
					limitOrderRepo.deleteById(id);
				}
			} else {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (refOrderRepo.existsById(id)) {
					refOrderRepo.deleteById(id);
				}
			}
			commentRepo.deleteById(entry.getValue().id());
		}
		for (Map.Entry<OrderTo, OrderCommentTo> entry : orderCommentsToRestore.entrySet()) {
			if (entry.getKey() instanceof LimitOrderTo) {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (limitOrderRepo.existsById(id)) {
					limitOrderRepo.deleteById(id);
				}
			} else {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (refOrderRepo.existsById(id)) {
					refOrderRepo.deleteById(id);
				}
			}
			commentConverter.toManagedEntity(entry.getValue());			
		}
	}
	
	@Transactional
	protected long submitNewOrderComment (OrderTo order, OrderCommentTo comment) {
		OrderCommentTo clearedId = ImmutableOrderCommentTo.builder()
				.from(comment)
				.id(0)
				.build();
			
		long newCommentId = 0;
		OrderTo updatedOrder;
		if (order instanceof LimitOrderTo) {
			newCommentId = commentController.postLimitOrderComment(order.id(), clearedId);	
			updatedOrder = ImmutableLimitOrderTo.builder()
					.from(order)
					.version(order.version()+1)
					.build();
		} else {
			newCommentId = commentController.postReferenceOrderComment(order.id(), clearedId);
			updatedOrder = ImmutableReferenceOrderTo.builder()
					.from(order)
					.version(order.version()+1)
					.build();			
		}
		OrderCommentTo withId = ImmutableOrderCommentTo.builder()
				.from(comment)
				.id(newCommentId)
				.build();
		orderCommentsToDelete.put(updatedOrder, withId);
		return newCommentId;
	}
	
	@Transactional
	protected void updateOrderComment (OrderTo order, OrderCommentTo oldComment, OrderCommentTo newComment) {
		if (order instanceof LimitOrderTo) {
			OrderTo orderVersionInc = ImmutableLimitOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			orderCommentsToRestore.put(orderVersionInc, oldComment);
			commentController.updateLimitOrderComment(order.id(), newComment.id(), newComment);				
		} else {
			OrderTo orderVersionInc = ImmutableReferenceOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			orderCommentsToRestore.put(orderVersionInc, oldComment);
			commentController.updateReferenceOrderComment(order.id(), newComment.id(), newComment);			
		}
	}
	
	@Transactional
	protected void deleteOrderComment (OrderTo order, OrderCommentTo oldComment) {
		if (order instanceof LimitOrderTo) {
			OrderTo orderVersionInc = ImmutableLimitOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			orderCommentsToRestore.put(orderVersionInc, oldComment);
			commentController.deleteLimitOrderComment(order.id(), oldComment.id());		
		} else {
			OrderTo orderVersionInc = ImmutableReferenceOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			orderCommentsToRestore.put(orderVersionInc, oldComment);
			commentController.deleteReferenceOrderComment(order.id(), oldComment.id());			
		}
	}
	
	@Test
	public void testGetLimitOrderSpecificComment() {
		OrderCommentTo comment = commentController.getCommentLimitOrder(TestLimitOrder.TEST_ORDER_1B.getEntity().id(), 
				TestOrderComment.TEST_COMMENT_1.getEntity().id());
		assertThat(comment).isEqualTo(TestOrderComment.TEST_COMMENT_1.getEntity());
	}
	
	@Test
	public void testGetLimitOrderAllComments() {
		List<Long> commentIdsFromController = commentController.getCommentsLimitOrders(TestLimitOrder.TEST_ORDER_1B.getEntity().id()).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> commentIdsTestData = TestLimitOrder.TEST_ORDER_1B.getEntity().orderCommentIds();
		assertThat(commentIdsFromController).containsExactlyInAnyOrderElementsOf(commentIdsTestData);
	}

	@Test
	public void testGetReferenceOrderSpecificComment() {
		OrderCommentTo comment = commentController.getCommentReferenceOrder(TestReferenceOrder.TEST_ORDER_1B.getEntity().id(), 
				TestOrderComment.TEST_COMMENT_7.getEntity().id());
		assertThat(comment).isEqualTo(TestOrderComment.TEST_COMMENT_7.getEntity());
	}
	
	@Test
	public void testGetReferenceOrderAllComments() {
		List<Long> commentIdsFromController = commentController.getCommentsReferenceOrders(TestReferenceOrder.TEST_ORDER_1B.getEntity().id()).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> commentIdsTestData = TestReferenceOrder.TEST_ORDER_1B.getEntity().orderCommentIds();
		assertThat(commentIdsFromController).containsExactlyInAnyOrderElementsOf(commentIdsTestData);
	}
	
	@Test
	public void testPostLimitOrderComment () {
		long newCommentId = submitNewOrderComment (TestLimitOrder.TEST_ORDER_1B.getEntity(), TestOrderComment.TEST_COMMENT_FOR_INSERT.getEntity());
		Optional<OrderComment> newComment = commentRepo.findById(newCommentId);
		assertThat(newComment).isNotEmpty();
		assertThat(newComment.get().getCommentText()).isEqualTo(TestOrderComment.TEST_COMMENT_FOR_INSERT.getEntity().commentText());
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_ORDER_1B.getEntity().version()+1);
		assertThat(order.get().getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(newCommentId);
	}

	@Test
	public void testPostLimitOrderCommentForCancellation () {
		long newCommentId = submitNewOrderComment (TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity(), TestOrderComment.TEST_COMMENT_FOR_INSERT_ACTION_CANCEL.getEntity());
		Optional<OrderComment> newComment = commentRepo.findById(newCommentId);
		assertThat(newComment).isNotEmpty();
		assertThat(newComment.get().getCommentText()).isEqualTo(TestOrderComment.TEST_COMMENT_FOR_INSERT_ACTION_CANCEL.getEntity().commentText());
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity().id());
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+1);
		assertThat(order.get().getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(newCommentId);
	}
	
	@Test
	public void testPostReferenceOrderComment () {
		long newCommentId = submitNewOrderComment (TestReferenceOrder.TEST_ORDER_1B.getEntity(), TestOrderComment.TEST_COMMENT_FOR_INSERT.getEntity());
		Optional<OrderComment> newComment = commentRepo.findById(newCommentId);
		assertThat(newComment).isNotEmpty();
		assertThat(newComment.get().getCommentText()).isEqualTo(TestOrderComment.TEST_COMMENT_FOR_INSERT.getEntity().commentText());
		Optional<ReferenceOrder> order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_ORDER_1B.getEntity().version()+1);
		assertThat(order.get().getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(newCommentId);
	}

	@Test
	public void testUpdateLimitOrderComment () {
		OrderCommentTo newCommentText = ImmutableOrderCommentTo.builder()
				.from(TestOrderComment.TEST_COMMENT_1.getEntity())
				.commentText("Updated Comment Text")
				.build();
		updateOrderComment(TestLimitOrder.TEST_ORDER_1B.getEntity(), TestOrderComment.TEST_COMMENT_1.getEntity(), newCommentText);
		Optional<OrderComment> updatedComment = commentRepo.findById(TestOrderComment.TEST_COMMENT_1.getEntity().id());
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(updatedComment).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_ORDER_1B.getEntity().version()+1);		
		assertThat(updatedComment.get().getCommentText()).isEqualTo(newCommentText.commentText());
		assertThat(order.get().getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestOrderComment.TEST_COMMENT_1.getEntity().id());
	}
	
	@Test
	public void testUpdateReferenceOrderComment () {
		OrderCommentTo newCommentText = ImmutableOrderCommentTo.builder()
				.from(TestOrderComment.TEST_COMMENT_7.getEntity())
				.commentText("Updated Comment Text")
				.build();
		updateOrderComment(TestReferenceOrder.TEST_ORDER_1B.getEntity(), TestOrderComment.TEST_COMMENT_7.getEntity(), newCommentText);
		Optional<OrderComment> updatedComment = commentRepo.findById(TestOrderComment.TEST_COMMENT_7.getEntity().id());
		Optional<ReferenceOrder> order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(updatedComment).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_ORDER_1B.getEntity().version()+1);
		assertThat(updatedComment.get().getCommentText()).isEqualTo(newCommentText.commentText());
		assertThat(order.get().getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestOrderComment.TEST_COMMENT_7.getEntity().id());
	}
	
	@Test
	public void testDeleteLimitOrderComment () {
		deleteOrderComment (TestLimitOrder.TEST_ORDER_1B.getEntity(), TestOrderComment.TEST_COMMENT_1.getEntity());
		Optional<OrderComment> updatedComment = commentRepo.findById(TestOrderComment.TEST_COMMENT_1.getEntity().id());
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(updatedComment).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_ORDER_1B.getEntity().version()+1);
		assertThat(updatedComment.get().getLifecycleStatus().getId()).isEqualTo(DefaultReference.LIFECYCLE_STATUS_DELETED.getEntity().id());
		// we expect the order comment to still be associated with the order after it status is moved to deleted.
		assertThat(order.get().getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestOrderComment.TEST_COMMENT_1.getEntity().id());		
	}
	
	@Test
	public void testDeleteReferenceOrderComment () {
		deleteOrderComment (TestReferenceOrder.TEST_ORDER_1B.getEntity(), TestOrderComment.TEST_COMMENT_7.getEntity());
		Optional<OrderComment> updatedComment = commentRepo.findById(TestOrderComment.TEST_COMMENT_7.getEntity().id());
		Optional<ReferenceOrder> order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(updatedComment).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_ORDER_1B.getEntity().version()+1);
		assertThat(updatedComment.get().getLifecycleStatus().getId()).isEqualTo(DefaultReference.LIFECYCLE_STATUS_DELETED.getEntity().id());
		// we expect the order comment to still be associated with the order after it status is moved to deleted.
		assertThat(order.get().getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestOrderComment.TEST_COMMENT_7.getEntity().id());		
	}
}
