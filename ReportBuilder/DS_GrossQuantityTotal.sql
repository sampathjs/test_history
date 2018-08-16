--table title "GrossQuantityTotal"

select dispatch_id, sum(gross_qty) gross_qty_total from crate_data group by dispatch_id