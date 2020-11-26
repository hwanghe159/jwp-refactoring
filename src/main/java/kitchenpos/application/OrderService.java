package kitchenpos.application;

import kitchenpos.dao.MenuDao;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderMenuDao;
import kitchenpos.dao.TableDao;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderMenu;
import kitchenpos.domain.Table;
import kitchenpos.dto.OrderCreateRequest;
import kitchenpos.dto.OrderMenuRequest;
import kitchenpos.dto.OrderStatusChangeRequest;
import kitchenpos.exception.MenuNotExistException;
import kitchenpos.exception.OrderNotExistException;
import kitchenpos.exception.TableEmptyException;
import kitchenpos.exception.TableNotExistenceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final MenuDao menuDao;
    private final OrderDao orderDao;
    private final OrderMenuDao orderMenuDao;
    private final TableDao tableDao;

    public OrderService(
            final MenuDao menuDao,
            final OrderDao orderDao,
            final OrderMenuDao orderMenuDao,
            final TableDao tableDao
    ) {
        this.menuDao = menuDao;
        this.orderDao = orderDao;
        this.orderMenuDao = orderMenuDao;
        this.tableDao = tableDao;
    }

    @Transactional
    public Order create(final OrderCreateRequest orderCreateRequest) {
        validateOrderCreateRequest(orderCreateRequest);

        List<OrderMenuRequest> orderMenuRequests = orderCreateRequest.getOrderMenuRequests();
        Long tableId = orderCreateRequest.getTableId();

        Order savedOrder = orderDao.save(new Order(tableId));

        Long orderId = savedOrder.getId();
        orderMenuRequests.stream()
            .map(request -> new OrderMenu(orderId, request.getMenuId(), request.getQuantity()))
            .forEach(orderMenuDao::save);

        return savedOrder;
    }

    private void validateOrderCreateRequest(OrderCreateRequest orderCreateRequest) {
        validateMenuExistence(orderCreateRequest.getOrderMenuRequests());
        validateTable(orderCreateRequest.getTableId());
    }

    private void validateMenuExistence(List<OrderMenuRequest> orderMenuRequests) {
        List<Long> menuIds = orderMenuRequests.stream()
                .map(OrderMenuRequest::getMenuId)
                .collect(Collectors.toList());

        if (orderMenuRequests.size() != menuDao.countByIdIn(menuIds)) {
            throw new MenuNotExistException();
        }
    }

    private void validateTable(Long tableId) {
        Table table = tableDao.findById(tableId)
            .orElseThrow(TableNotExistenceException::new);

        if (table.isEmpty()) {
            throw new TableEmptyException();
        }
    }

    public List<Order> list() {
        return orderDao.findAll();
    }

    @Transactional
    public Order changeOrderStatus(final Long orderId, final OrderStatusChangeRequest orderStatusChangeRequest) {
        final Order savedOrder = orderDao.findById(orderId)
                .orElseThrow(OrderNotExistException::new);

        savedOrder.changeOrderStatus(orderStatusChangeRequest.getOrderStatus());

        orderDao.save(savedOrder);
        return savedOrder;
    }
}
