package pl.tomaszko.cheapskountant.receipt.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class ReceiptPersistenceTest {

    @Container
    @ServiceConnection
    static final MariaDBContainer MARIADB = new MariaDBContainer("mariadb:11.8");

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private EntityManager entityManager;

    @Test
    void roundTripsAReceiptWithoutImageBytes() {
        for (var entity : entityManager.getMetamodel().getEntities()) {
            for (var attribute : entity.getSingularAttributes()) {
                assertThat(attribute.getJavaType()).isNotEqualTo(byte[].class);
            }
        }

        ReceiptDraft draft = ReceiptFixtures.validDraft();
        ReceiptEntity saved = receiptRepository.saveAndFlush(receiptMapper.toEntity(new ReceiptDraft(
                draft.documentType(),
                new ReceiptDraft.Source("page-1.jpg, page-2.jpg", draft.source().rawText()),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                draft.discountSummary(),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines())));
        entityManager.clear();

        ReceiptEntity loaded = receiptRepository.findById(saved.id).orElseThrow();
        assertThat(loaded.createdAt).isEqualTo(loaded.updatedAt);
        assertThat(loaded.seller.createdAt).isEqualTo(loaded.seller.updatedAt);
        assertThat(loaded.header.createdAt).isEqualTo(loaded.header.updatedAt);
        assertThat(loaded.items.get(0).createdAt).isEqualTo(loaded.items.get(0).updatedAt);
        assertThat(loaded.taxSummaries.get(0).createdAt).isEqualTo(loaded.taxSummaries.get(0).updatedAt);
        assertThat(loaded.totals.createdAt).isEqualTo(loaded.totals.updatedAt);
        assertThat(loaded.payments.get(0).createdAt).isEqualTo(loaded.payments.get(0).updatedAt);
        assertThat(loaded.documentType).isEqualTo("fiscal_receipt");
        assertThat(loaded.sourceFileName).isEqualTo("page-1.jpg, page-2.jpg");
        assertThat(loaded.sourceRawText).isEqualTo("PARAGON FISKALNY");
        assertThat(loaded.seller.taxId).isEqualTo("1234567890");
        assertThat(loaded.items.get(0).lineNo).isEqualTo(1);
        assertThat(loaded.items.get(0).description).isEqualTo("Milk");
        assertThat(loaded.items.get(0).category.name()).isEqualTo("Unknown");
        assertThat(receiptMapper.toResponse(loaded).items().get(0).category()).isEqualTo("Unknown");
        assertThat(loaded.taxSummaries).hasSize(1);
        assertThat(loaded.payments.get(0).method).isEqualTo("card");
        assertThat(loaded.header.currency).isEqualTo("PLN");
        assertThat(loaded.items.get(0).discountDescription).isNull();
        assertThat(loaded.items.get(0).discountTotal).isNull();
        assertThat(loaded.discountSummary).isNull();
    }

    @Test
    void roundTripsItemDiscountAndDiscountSummary() {
        ReceiptEntity saved = receiptRepository.saveAndFlush(receiptMapper.toEntity(ReceiptFixtures.discountedDraft()));
        entityManager.clear();

        ReceiptEntity loaded = receiptRepository.findById(saved.id).orElseThrow();
        assertThat(loaded.items).extracting(item -> item.description)
                .containsExactly("NapLiptIcTeMIX1,5L", "ChipsyLay soveB110g", "But Plastik kaucja");
        assertThat(loaded.items.get(1).lineTotal).isEqualByComparingTo("26.07");
        assertThat(loaded.items.get(1).discountDescription).isEqualTo("OPUST");
        assertThat(loaded.items.get(1).discountTotal).isEqualByComparingTo("-8.69");
        assertThat(loaded.items.get(0).discountTotal).isNull();
        assertThat(loaded.items.get(2).itemType).isEqualTo("packaging");
        assertThat(loaded.discountSummary.description).isEqualTo("OPUSTY ŁĄCZNIE");
        assertThat(loaded.discountSummary.total).isEqualByComparingTo("-8.69");
        assertThat(loaded.discountSummary.createdAt).isEqualTo(loaded.discountSummary.updatedAt);

        var response = receiptMapper.toResponse(loaded);
        assertThat(response.items().get(1).discount().total()).isEqualTo("-8.69");
        assertThat(response.discountSummary().description()).isEqualTo("OPUSTY ŁĄCZNIE");
    }
}
