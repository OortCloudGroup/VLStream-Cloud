package com.ruoyi.vlstream.test.vlstream.service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmServiceImpl;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@Tag("dev")
class AlgorithmRepositoryMoveTest {
 private IVlsAlgorithmRepositoryService repos;
 private VlsAlgorithmServiceImpl service;
 @BeforeEach void setup() {
  repos=mock(IVlsAlgorithmRepositoryService.class);
  service=spy(new VlsAlgorithmServiceImpl(mock(VlsAlgorithmMapper.class),repos));
  Algorithm existing=algorithm(1L); existing.setTenantId("a");
  doReturn(existing).when(service).getOne(any(Wrapper.class));
  doReturn(0L).when(service).count(any(Wrapper.class));
  doReturn(true).when(service).updateById(any(Algorithm.class));
 }
 @Test void nestedMoveUpdatesBothCountsPreservesTenant() {
  when(repos.list(any(Wrapper.class))).thenReturn(Arrays.asList(category(2L,3L,"a",1),category(3L,0L,"a",1)));
  Algorithm update=algorithm(2L); update.setTenantId("spoofed");
  assertTrue(service.updateAlgorithm(update)); assertEquals("a",update.getTenantId());
  verify(repos).updateAlgorithmCount(1L); verify(repos).updateAlgorithmCount(2L);
 }
 @Test void absentOrInaccessibleTargetRejected() {
  when(repos.list(any(Wrapper.class))).thenReturn(Collections.emptyList()); reject();
 }
 @Test void crossTenantTargetRejected() {
  when(repos.list(any(Wrapper.class))).thenReturn(Collections.singletonList(category(2L,0L,"b",1))); reject();
 }
 @Test void disabledAncestorRejected() {
  when(repos.list(any(Wrapper.class))).thenReturn(Arrays.asList(category(2L,3L,"a",1),category(3L,0L,"a",0))); reject();
 }
 @Test void omittedRepositoryKeepsMembership() {
  Algorithm update=algorithm(null); assertTrue(service.updateAlgorithm(update)); assertEquals(1L,update.getRepositoryId()); verifyNoInteractions(repos);
 }
 @Test void duplicateNameRejected() {
  when(repos.list(any(Wrapper.class))).thenReturn(Collections.singletonList(category(2L,0L,"a",1)));
  doReturn(1L).when(service).count(any(Wrapper.class)); reject();
 }
 @Test void failedWriteDoesNotRefreshCounters() {
  when(repos.list(any(Wrapper.class))).thenReturn(Collections.singletonList(category(2L,0L,"a",1)));
  doReturn(false).when(service).updateById(any(Algorithm.class));
  assertFalse(service.updateAlgorithm(algorithm(2L))); verify(repos,never()).updateAlgorithmCount(anyLong());
 }
 @Test void legacyUpdateRoutesThroughValidatedService() {
  IVlsAlgorithmService api = mock(IVlsAlgorithmService.class);
  com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmController controller =
   new com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmController(api, mock(AlgorithmCoverService.class));
  Algorithm update = algorithm(2L);
  controller.update(update); controller.submit(update);
  verify(api, times(2)).updateAlgorithm(update);
  verify(api, never()).updateById(any(Algorithm.class));
  verify(api, never()).saveOrUpdate(any(Algorithm.class));
 }
 private void reject() { assertThrows(ServiceException.class,()->service.updateAlgorithm(algorithm(2L))); verify(service,never()).updateById(any(Algorithm.class)); }
 private Algorithm algorithm(Long id) { Algorithm a=new Algorithm(); a.setId(7L); a.setName("test"); a.setRepositoryId(id); return a; }
 private AlgorithmRepository category(Long id,Long parent,String tenant,int status) { AlgorithmRepository r=new AlgorithmRepository(); r.setId(id); r.setParentId(parent); r.setTenantId(tenant); r.setStatus(status); return r; }
}
