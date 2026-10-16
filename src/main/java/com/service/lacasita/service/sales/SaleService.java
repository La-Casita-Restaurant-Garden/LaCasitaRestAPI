package com.service.lacasita.service.sales;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.service.lacasita.repository.SalesRepository;
import com.service.lacasita.dto.SalesResponse;
import com.service.lacasita.entity.Sales;
import com.service.lacasita.entity.UserModel;

@Service
public class SaleService {

    private final SalesRepository salesRepository;

    public SaleService(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public Page<SalesResponse> findAllSales(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("idventa").descending());

        return salesRepository.findAll(pageable).map(this::aResponse);
    }


    public Page<SalesResponse> findAllSalesByCustomerId(Long id, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha", "idventa").descending());

        return salesRepository.findAllSalesByCustomerId(id, pageable).map(this::aResponse);
    }

    private SalesResponse aResponse(Sales s) {
        return new SalesResponse(
                s.getIdventa(),
                s.getFecha(),
                s.getTotal(),
                s.getCustomer().getName() + " " + (s.getCustomer().getLast_name() != null ? s.getCustomer().getLast_name() : ""),
                s.getStatus(),
                s.getOrder().getOrderId(),
                s.getOrder().getOrder_number(),
                s.getOrder().getPaymentMethod().getName(),
                s.getIdusuario(),
                nombreDe(s.getOrder().getCreatedBy()));
    }

    private String nombreDe(UserModel u) {
        if (u == null) return null;
        return u.getLastname() == null || u.getLastname().isBlank()
                ? u.getName() : u.getName() + " " + u.getLastname();
    }

    /**
     * Al crear una venta, se invalidan los caches de gráficas y KPIs.
     */
    @CacheEvict(value = {"salesChart", "salesChartDaily", "dashboardCharts", "dashboardKpis"}, allEntries = true)
    public Sales createSale(Sales sale) {
        return salesRepository.save(sale);
    }
}
