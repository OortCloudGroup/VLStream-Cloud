package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.servlet.model.*;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.vlstream.test.vlstream.controller.VlsComputeController;
import java.util.*;
import javax.servlet.http.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Sa annotation/MVC guards; isolated fake roles, token storage and node services. */
@Tag("dev")
class ComputeControllerPermissionTest {
    private SaTokenContext previousContext;
    private SaTokenDao previousDao;
    private StpInterface previousPermissions;
    private SaTokenDaoDefaultImpl memory;
    private Set<String> permissions;
    private ComputeNodeService nodes;
    private ComputeMarketService market;
    private MyTrainingTasksService tasks;
    private ComputeProviderStateService providerState;
    private MockMvc mvc;
    private String token;
    private static final String BODY="{\"name\":\"fixture\",\"host\":\"fixture.invalid\"}";
    @BeforeEach void setup() {
        previousContext=SaManager.getSaTokenContext();previousDao=SaManager.getSaTokenDao();previousPermissions=SaManager.getStpInterface();
        memory=new SaTokenDaoDefaultImpl();SaManager.setSaTokenDao(memory);SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());permissions=new HashSet<>();
        SaManager.setStpInterface(new StpInterface(){
            public List<String> getPermissionList(Object id,String type){return new ArrayList<>(permissions);}
            public List<String> getRoleList(Object id,String type){return Collections.emptyList();}
        });
        context(new MockHttpServletRequest(),new MockHttpServletResponse());StpUtil.login("compute-fixture-only");token=StpUtil.getTokenValue();SaTokenContextForThreadLocalStorage.clearBox();
        nodes=mock(ComputeNodeService.class);market=mock(ComputeMarketService.class);tasks=mock(MyTrainingTasksService.class);
        when(nodes.save(any(),any())).thenReturn(new ComputeNode());when(nodes.probe(anyLong())).thenReturn(new ComputeNode());
        when(nodes.list()).thenReturn(Collections.emptyList());when(market.list(anyBoolean())).thenReturn(Collections.emptyList());
        when(tasks.page(anyInt(),anyInt(),any(),any(),any(),any())).thenReturn(new MyTrainingTasksService.Page());
        providerState=mock(ComputeProviderStateService.class);
        VlsComputeController controller=new VlsComputeController(nodes,mock(CloudTrainingService.class),market,tasks,
            mock(ComputeProviderSettingsService.class),providerState,mock(ComputeCostObservationService.class),mock(ComputeHealthRefreshService.class),mock(ComputeProviderInventoryService.class));
        SaInterceptor guards=new SaInterceptor();
        mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new Errors()).addInterceptors(new HandlerInterceptor(){
            public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler)throws Exception{context(request,response);return guards.preHandle(request,response,handler);}
            public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception failure){SaTokenContextForThreadLocalStorage.clearBox();}
        }).build();
    }
    private static void context(HttpServletRequest request,HttpServletResponse response){SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),new SaResponseForServlet(response),new SaStorageForServlet(request));}
    @AfterEach void cleanup(){SaTokenContextForThreadLocalStorage.clearBox();SaManager.setStpInterface(previousPermissions);SaManager.setSaTokenDao(previousDao);SaManager.setSaTokenContext(previousContext);memory.endRefreshThread();}
    private List<MockHttpServletRequestBuilder> mutations(){return Arrays.asList(post("/vlsCompute/nodes").contentType(MediaType.APPLICATION_JSON).content(BODY),put("/vlsCompute/nodes/1").contentType(MediaType.APPLICATION_JSON).content(BODY),post("/vlsCompute/nodes/1/probe"),delete("/vlsCompute/nodes/1"));}
    private MockHttpServletRequestBuilder logged(MockHttpServletRequestBuilder request){return request.header(SaManager.getConfig().getTokenName(),token);}
    @Test void ordinaryPublisherPowerAndObserverRolesCannotMutateOrProbeNodes()throws Exception {
        for(String permission:Arrays.asList("","vls:compute:publish","vls:compute:power","vls:compute:observe")) {
            permissions.clear();if(!permission.isEmpty())permissions.add(permission);
            for(MockHttpServletRequestBuilder request:mutations())mvc.perform(logged(request)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(nodes);
    }
    @Test void existingConfigurePermissionAllowsAllFourOperations()throws Exception {
        permissions.add("vls:compute:configure");for(MockHttpServletRequestBuilder request:mutations())mvc.perform(logged(request)).andExpect(status().isOk());
        verify(nodes).save(isNull(),any());verify(nodes).save(eq(1L),any());verify(nodes).probe(1L);verify(nodes).delete(1L);
    }
    private List<MockHttpServletRequestBuilder> ownMutations(){return Arrays.asList(post("/vlsCompute/my-nodes").contentType(MediaType.APPLICATION_JSON).content(BODY),put("/vlsCompute/my-nodes/1").contentType(MediaType.APPLICATION_JSON).content(BODY),post("/vlsCompute/my-nodes/1/probe"),delete("/vlsCompute/my-nodes/1"));}
    @Test void ordinaryLoginCanUseOnlyTheSelfServiceRoutesWithoutProviderPermission()throws Exception {
        for(MockHttpServletRequestBuilder request:ownMutations())mvc.perform(logged(request)).andExpect(status().isOk());
        verify(nodes).saveOwn(isNull(),any());verify(nodes).saveOwn(eq(1L),any());verify(nodes).probeOwn(1L);verify(nodes).deleteOwn(1L);
        verify(nodes,never()).save(any(),any());verifyNoInteractions(market,providerState);
    }
    @Test void loggedOutSelfServiceRequestsAreRejectedBeforeNodeServices()throws Exception {
        for(MockHttpServletRequestBuilder request:ownMutations())mvc.perform(request).andExpect(status().isUnauthorized());verifyNoInteractions(nodes);
    }
    @Test void selfServiceProbeAndDeleteCannotUseAdministratorScopeToBypassOwnership()throws Exception {
        permissions.add("*:*:*");doThrow(new com.ruoyi.common.exception.ServiceException("fixture owner denied",403)).when(nodes).probeOwn(1L);
        doThrow(new com.ruoyi.common.exception.ServiceException("fixture owner denied",403)).when(nodes).deleteOwn(1L);
        mvc.perform(logged(post("/vlsCompute/my-nodes/1/probe"))).andExpect(status().isForbidden());
        mvc.perform(logged(delete("/vlsCompute/my-nodes/1"))).andExpect(status().isForbidden());verify(nodes,never()).probe(anyLong());verify(nodes,never()).delete(anyLong());
    }
    @Test void loggedOutMutationsAreDeniedBeforeNodeServices()throws Exception {
        for(MockHttpServletRequestBuilder request:mutations())mvc.perform(request).andExpect(status().isUnauthorized());verifyNoInteractions(nodes);
    }
    @Test void readListsRemainSeparateFromManagerPermission()throws Exception {
        for(String path:Arrays.asList("/vlsCompute/nodes","/vlsCompute/market","/vlsCompute/selection","/vlsCompute/my-trainings"))mvc.perform(logged(get(path))).andExpect(status().isOk());
        verify(nodes).list();verify(market).list(false);verify(market).list(true);verify(tasks).page(1,20,null,null,null,null);
    }
    @Test void publicClaimStillUsesItsConsumerLoginGuardWithoutConfigureRole()throws Exception {
        when(market.claim(1L)).thenReturn("fixture claim");mvc.perform(logged(post("/vlsCompute/nodes/1/claim"))).andExpect(status().isOk());verify(market).claim(1L);
        mvc.perform(post("/vlsCompute/nodes/1/claim")).andExpect(status().isUnauthorized());verifyNoInteractions(nodes);
    }
    @Test void managerPowerAndProviderRoutesCannotBypassOwnershipViaConsumerClaim()throws Exception {
        permissions.add("vls:compute:power");doThrow(new com.ruoyi.common.exception.ServiceException("fixture owner denied",47003)).when(nodes).getManaged(1L);
        mvc.perform(logged(post("/vlsCompute/nodes/1/provider-state"))).andExpect(status().isForbidden());
        mvc.perform(logged(post("/vlsCompute/nodes/1/power/start"))).andExpect(status().isForbidden());verifyNoInteractions(market,providerState);
        verify(nodes,never()).authorizePaidOperation(anyLong());
        ComputeNode company=new ComputeNode();company.setResourceScope("PLATFORM");when(nodes.get(1L)).thenReturn(company);
        when(providerState.refresh(1L,false)).thenReturn("fixture public consumer query");
        mvc.perform(logged(post("/vlsCompute/public/nodes/1/provider-state"))).andExpect(status().isOk());verify(providerState).refresh(1L,false);
        verify(nodes,times(2)).getManaged(1L);
    }
    @RestControllerAdvice static class Errors {
        @ExceptionHandler(NotPermissionException.class) ResponseEntity<String> permission(){return ResponseEntity.status(403).body("fixture denied");}
        @ExceptionHandler(NotLoginException.class) ResponseEntity<String> login(){return ResponseEntity.status(401).body("fixture login required");}
        @ExceptionHandler(com.ruoyi.common.exception.ServiceException.class) ResponseEntity<String> ownership(){return ResponseEntity.status(403).body("fixture owner denied");}
    }
}
