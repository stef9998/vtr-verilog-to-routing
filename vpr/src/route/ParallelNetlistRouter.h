#pragma once

/** @file Parallel case for NetlistRouter. Builds a \ref PartitionTree from the
 * netlist according to net bounding boxes. Tree nodes are then routed in parallel
 * using tbb::task_group. Each task routes the nets inside a node serially and then adds
 * its child nodes to the task queue. This approach is serially equivalent & deterministic,
 * but it can reduce QoR in congested cases [0].
 *
 * Note that the parallel router does not support graphical router breakpoints.
 *
 * [0]: F. Koşar, "A net-decomposing parallel FPGA router", MS thesis, UofT ECE, 2023 */
#include "netlist_routers.h"

#include <tbb/task_group.h>

/** Parallel impl for NetlistRouter.
 * Holds enough context members to glue together ConnectionRouter and net routing functions,
 * such as \ref route_net. Keeps the members in thread-local storage where needed,
 * i.e. ConnectionRouters and RouteIterResults-es.
 * See \ref route_net. */
template<typename HeapType>
class ParallelNetlistRouter : public NetlistRouter {
  public:
    ParallelNetlistRouter(
        const Netlist<>& net_list,
        const RouterLookahead* router_lookahead,
        const t_router_opts& router_opts,
        CBRR& connections_inf,
        NetPinsMatrix<float>& net_delay,
        const ClusteredPinAtomPinsLookup& netlist_pin_lookup,
        std::shared_ptr<SetupHoldTimingInfo> timing_info,
        NetPinTimingInvalidator* pin_timing_invalidator,
        route_budgets& budgeting_inf,
        const RoutingPredictor& routing_predictor,
        const vtr::vector<ParentNetId, std::vector<std::unordered_map<RRNodeId, int>>>& choking_spots,
        bool is_flat)
        : _routers_th(_make_router(router_lookahead, is_flat))
        , _net_list(net_list)
        , _router_opts(router_opts)
        , _connections_inf(connections_inf)
        , _net_delay(net_delay)
        , _netlist_pin_lookup(netlist_pin_lookup)
        , _timing_info(timing_info)
        , _pin_timing_invalidator(pin_timing_invalidator)
        , _budgeting_inf(budgeting_inf)
        , _routing_predictor(routing_predictor)
        , _choking_spots(choking_spots)
        , _is_flat(is_flat) {}
    ~ParallelNetlistRouter() {}

    /** Run a single iteration of netlist routing for this->_net_list. This usually means calling
     * \ref route_net for each net, which will handle other global updates.
     * \return RouteIterResults for this iteration. */
    RouteIterResults route_netlist(int itry, float pres_fac, float worst_neg_slack);
    void set_rcv_enabled(bool x);
    void set_timing_info(std::shared_ptr<SetupHoldTimingInfo> timing_info);

  private:
    /** A single task to route nets inside a PartitionTree node and add tasks for its child nodes to task group \p g. */
    void route_partition_tree_node(tbb::task_group& g, PartitionTreeNode& node, int itry, float pres_fac, float worst_neg_slack);

    ConnectionRouter<HeapType> _make_router(const RouterLookahead* router_lookahead, bool is_flat) {
        auto& device_ctx = g_vpr_ctx.device();
        auto& route_ctx = g_vpr_ctx.mutable_routing();

        return ConnectionRouter<HeapType>(
            device_ctx.grid,
            *router_lookahead,
            device_ctx.rr_graph.rr_nodes(),
            &device_ctx.rr_graph,
            device_ctx.rr_rc_data,
            device_ctx.rr_graph.rr_switch(),
            route_ctx.rr_node_route_inf,
            is_flat);
    }

    /* Context fields */
    tbb::enumerable_thread_specific<ConnectionRouter<HeapType>> _routers_th;
    const Netlist<>& _net_list;
    const t_router_opts& _router_opts;
    CBRR& _connections_inf;
    tbb::enumerable_thread_specific<RouteIterResults> _results_th;
    NetPinsMatrix<float>& _net_delay;
    const ClusteredPinAtomPinsLookup& _netlist_pin_lookup;
    std::shared_ptr<SetupHoldTimingInfo> _timing_info;
    NetPinTimingInvalidator* _pin_timing_invalidator;
    route_budgets& _budgeting_inf;
    const RoutingPredictor& _routing_predictor;
    const vtr::vector<ParentNetId, std::vector<std::unordered_map<RRNodeId, int>>>& _choking_spots;
    bool _is_flat;
};

#include "ParallelNetlistRouter.tpp"
